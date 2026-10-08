package com.aaax.server.config.security;

import com.aaax.core.utils.InstantUtil;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.config.extension.GrantTypeExtension;
import com.aaax.server.config.redis.RedisKey;
import com.aaax.server.config.security.jwt.ClientCredentialsJwt;
import com.aaax.server.config.security.jwt.Jwt;
import com.aaax.server.config.security.jwt.JwtPayload;
import com.aaax.server.config.security.jwt.RegisteredClientMetadata;
import com.aaax.server.entity.enu.UserTokenType;
import com.aaax.server.entity.po.user_token.UserToken;
import com.aaax.server.repository.UserTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Instant;
import java.util.*;

@Slf4j
public class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

    @Autowired
    private RedisUtil redisUtil;
    @Value("${aaax.security.refresh-token.history-count}")
    private Integer refreshTokenHistoryCount;
    @Value("${aaax.security.server.expiry-time}")
    private Integer serverTokenExpiryTime;
    @Autowired
    private RegisteredClientRepository registeredClientRepository;
    @Autowired
    private UserTokenRepository userTokenRepository;

    /** Authorization codes have no access token yet — qs Redis JWT mapping cannot store them. */
    private final OAuth2AuthorizationService authorizationCodes = new InMemoryOAuth2AuthorizationService();

    @Override
    public void save(OAuth2Authorization authorization) {
        if (authorization.getAccessToken() == null) {
            authorizationCodes.save(authorization);
            return;
        }
        try {
            RegisteredClientMetadata registeredClientMetadata = convertAuthorizationToRegisteredClientClass(authorization);
            if (authorization.getAuthorizationGrantType().equals(AuthorizationGrantType.CLIENT_CREDENTIALS)) {
                ClientCredentialsJwt jwt = this.convertAuthorizationToClientCredentialsJwtClass(authorization);
                redisUtil.set(this.getClientCredentialsKey(authorization.getPrincipalName()), jwt, serverTokenExpiryTime);
            } else {
                if (authorization.getAccessToken().getClaims() == null) {
                    log.warn("access token has no claims; skip Redis JWT mapping");
                    authorizationCodes.save(authorization);
                    return;
                }
                Jwt jwt = this.convertAuthorizationToJwtClass(authorization);
                jwt.setRegisteredClientMetadata(registeredClientMetadata);
                log.debug("stored oauth session sub={}", jwt.getPayload() == null ? null : jwt.getPayload().getSub());
                String expiredRefreshToken = authorization.getAttribute("refresh-token");
                if (expiredRefreshToken != null) {
                    this.expireRefreshToken(expiredRefreshToken);
                }
                RegisteredClient client = registeredClientRepository.findById(registeredClientMetadata.getId());
                if (client == null) {
                    log.warn("registered client {} missing; skip Redis JWT mapping", registeredClientMetadata.getId());
                    authorizationCodes.save(authorization);
                    return;
                }
                this.__doTokenStorageInRedis(jwt, client.getTokenSettings());
            }
        } catch (RuntimeException ex) {
            log.warn("Redis JWT mapping failed; keeping authorization in memory: {}", ex.toString(), ex);
            authorizationCodes.save(authorization);
        }
    }

    private void __doTokenStorageInRedis(Jwt jwt, TokenSettings tokenSettings) {
        redisUtil.set(this.getTokenKeyPerUser(jwt.getPayload().getSub()), jwt, tokenSettings.getAccessTokenTimeToLive().getSeconds());
        if (jwt.getRefreshToken() != null) {
            redisUtil.set(this.getRefreshTokenInRedis(jwt.getRefreshToken()), jwt, tokenSettings.getRefreshTokenTimeToLive().getSeconds());
        }
        if (jwt.getPayload() != null && jwt.getPayload().getMetadata() != null
                && jwt.getPayload().getMetadata().getSessionId() != null) {
            redisUtil.set(this.wsHash(jwt.getPayload().getMetadata().getSessionId()), jwt, tokenSettings.getAccessTokenTimeToLive().getSeconds());
        }
        log.debug("saved oauth session sub={}", jwt.getPayload() == null ? null : jwt.getPayload().getSub());
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        authorizationCodes.remove(authorization);
        if (authorization.getAccessToken() == null) {
            return;
        }
        Jwt jwt = convertAuthorizationToJwtClass(authorization);
        redisUtil.delete(getTokenKeyPerUser(jwt.getPayload().getSub()));
        if (jwt.getRefreshToken() != null) {
            redisUtil.delete(getRefreshTokenInRedis(jwt.getRefreshToken()));
        }
        log.debug("removed oauth session sub={}", jwt.getPayload() == null ? null : jwt.getPayload().getSub());
    }

    @Override
    public OAuth2Authorization findById(String userId) {
        OAuth2Authorization pending = authorizationCodes.findById(userId);
        if (pending != null) {
            return pending;
        }
        try {
            String tokenKeyPerUser = this.getTokenKeyPerUser(userId);
            log.debug("findById key={}", tokenKeyPerUser);
            Jwt jwt = JSONUtil.convertFromObject(redisUtil.getOrElseThrow(tokenKeyPerUser), Jwt.class);
            return this.__transformJwtToOAuth2Authorization(jwt);
        } catch (Exception ex) {
            log.info("-- RedisOAuth2AuthorizationService.findById exception : {}", ex.getMessage());
            return null; // ___ pass to re-generate flow
        }
    }

    private OAuth2Authorization __doCompensationControlFetchingDb(String value, UserTokenType userTokenType) {
        switch (userTokenType) {
            case REFRESH_TOKEN -> {
                Optional<UserToken> __token = userTokenRepository.findByTokenValueAndTokenType(value, userTokenType.name());
                if (__token.isEmpty()) {
                    return null;
                }
                if (InstantUtil.isExpired(__token.get().getExpireAt())) {
                    return null;
                }
                Jwt jwt = JSONUtil.convertFromObject(__token.get().getValue(), Jwt.class);
                return this.__transformJwtToOAuth2Authorization(jwt);
            }
        }
        return null; // ___ pass to re-generate flow
    }

    @Override
    public OAuth2Authorization findByToken(String refreshToken, OAuth2TokenType tokenType) {
        OAuth2Authorization pending = authorizationCodes.findByToken(refreshToken, tokenType);
        if (pending != null) {
            return pending;
        }
        try {
            log.debug("findByToken type={}", tokenType == null ? null : tokenType.getValue());
            String refreshTokenInRedis = getRefreshTokenInRedis(refreshToken);
            return this.__transformJwtToOAuth2Authorization(JSONUtil.convertFromObject(redisUtil.getOrElseThrow(refreshTokenInRedis), Jwt.class));
        } catch (Exception ex) {
            log.info("-- RedisOAuth2AuthorizationService.findByToken exception : {}", ex.getMessage());
            return this.__doCompensationControlFetchingDb(refreshToken, UserTokenType.REFRESH_TOKEN);// ___ pass to re-generate flow
        }
    }


    /**
     * Dto wrapper class, convert between java class and redis class
     * @param authorization - Spring Security OAuth2Authorization
     * @return Jwt.class --> used to store in Redis value
     */
    private Jwt convertAuthorizationToJwtClass(OAuth2Authorization authorization) {
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        OAuth2Authorization.Token<OidcIdToken> token = authorization.getToken(OidcIdToken.class);

        JwtPayload jwtPayload = JSONUtil.convertValue(Objects.requireNonNull(accessToken.getClaims()), JwtPayload.class); //__ jwt payload in jwt.io
        RegisteredClientMetadata client = RegisteredClientMetadata.builder()
                .id(authorization.getRegisteredClientId())
                .build();
        var jwtBuilder = Jwt.builder()
                .accessToken(Objects.requireNonNull(accessToken.getToken().getTokenValue()))
                .accessTokenIssuedAt(accessToken.getToken().getIssuedAt())
                .accessTokenExpiresAt(accessToken.getToken().getExpiresAt())
                .principalName(Objects.requireNonNull(authorization.getPrincipalName()))
                .payload(jwtPayload)
                .authorizationGrantType(authorization.getAuthorizationGrantType().getValue())
                .scopes(authorization.getAuthorizedScopes())
                .expiresIn(Objects.requireNonNull(authorization.getAccessToken().getToken().getExpiresAt()).getEpochSecond())
                .registeredClientMetadata(client);
        if (refreshToken != null && refreshToken.getToken() != null) {
            jwtBuilder.refreshToken(refreshToken.getToken().getTokenValue())
                    .refreshTokenIssuedAt(refreshToken.getToken().getIssuedAt())
                    .refreshTokenExpiresAt(refreshToken.getToken().getExpiresAt());
        }
        if (token != null && token.getToken() != null) {
            jwtBuilder.idToken(token.getToken().getTokenValue());
        }
        return jwtBuilder.build();
    }

    private ClientCredentialsJwt convertAuthorizationToClientCredentialsJwtClass(OAuth2Authorization authorization) {
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        return ClientCredentialsJwt.builder()
                .accessToken(Objects.requireNonNull(accessToken.getToken().getTokenValue()))
                .build();
    }

    private RegisteredClientMetadata convertAuthorizationToRegisteredClientClass(OAuth2Authorization authorization) {
        return RegisteredClientMetadata.builder()
                .id(authorization.getRegisteredClientId())
                .build();
    }

    public void cleanUp(String userId) {
        String key = getTokenKeyPerUser(userId);
        redisUtil.delete(key);
        log.info("-- RedisOAuth2AuthorizationService cleanUp end: {}", key);
    }


    // ======= util method ========
    private OAuth2Authorization __transformJwtToOAuth2Authorization(Jwt jwt) {
        // __ client
        RegisteredClient client = registeredClientRepository.findById(jwt.getRegisteredClientMetadata().getId());
        log.debug("loaded registered client id={}", client == null ? null : client.getId());
        assert client != null;

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                jwt.getAccessToken(),
                jwt.getAccessTokenIssuedAt(),
                jwt.getAccessTokenExpiresAt(),
                client.getScopes()
        );

        // __ refreshToken
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                jwt.getRefreshToken(),
                jwt.getRefreshTokenIssuedAt(),
                jwt.getRefreshTokenExpiresAt()
        );

        OAuth2Authorization oauth2Authorization = OAuth2Authorization
                .withRegisteredClient(Objects.requireNonNull(client))
                .principalName(jwt.getPrincipalName())
                .authorizationGrantType(GrantTypeExtension.toAuthorizationGrantType(jwt.getAuthorizationGrantType()))
                .refreshToken(refreshToken)
                .accessToken(accessToken)
                .attribute("username", jwt.getPrincipalName())
                .attribute("refresh-token", jwt.getRefreshToken())    // for get old refresh token
                .build();
        return oauth2Authorization;
    }

    @NotNull
    private String getRefreshTokenInRedis(String refreshToken) {
        return RedisKey.USER_OAUTH_TOKENS_REFRESH_TOKEN.getKey().concat(refreshToken);
    }

    @NotNull
    private String getTokenKeyPerUser(String userId) {
        return RedisKey.USER_OAUTH_TOKENS.getKey().concat(userId);
    }

    @NotNull
    private String wsHash(String wsHash) {
        return RedisKey.USER_WS_HASH.getKey().concat(wsHash);
    }

    private void expireRefreshToken(String oldRefreshToken) {
        redisUtil.delete(getRefreshTokenInRedis(oldRefreshToken));
        userTokenRepository.findByTokenValueAndTokenType(oldRefreshToken, UserTokenType.REFRESH_TOKEN.name())
                .ifPresent(row -> {
                    row.setIsActive(false);
                    row.setExpireAt(Instant.now());
                    userTokenRepository.save(row);
                });
    }

    @NotNull
    private String getClientCredentialsKey(String clientId) {
        return RedisKey.USER_CLIENT_CREDENTIALS.getKey().concat(clientId);
    }

    @NotNull
    private String getRefreshTokenHistoryKey(String userId) {
        return RedisKey.USER_OAUTH_TOKENS_REFRESH_TOKEN_HISTORY.getKey().concat(userId);
    }
}
