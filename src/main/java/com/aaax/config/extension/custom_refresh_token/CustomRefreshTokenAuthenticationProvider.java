package com.aaax.config.extension.custom_refresh_token;


import com.aaax.core.utils.InstantUtil;
import com.aaax.core.utils.JSONUtil;
import com.aaax.config.extension.BaseAuthenticationProvider;
import com.aaax.config.extension.GrantTypeExtension;
import com.aaax.entity.po.user.User;
import com.aaax.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.*;

@Slf4j
public class CustomRefreshTokenAuthenticationProvider extends BaseAuthenticationProvider implements AuthenticationProvider {
    private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2";
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final AuthenticationManager authenticationManager;
    @Autowired
    private AuthenticationService authenticationService;

    public CustomRefreshTokenAuthenticationProvider(OAuth2AuthorizationService authorizationService,
                                                    OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
                                                    AuthenticationManager authenticationManager) {
        Assert.notNull(authorizationService, "authorizationService cannot be null");
        Assert.notNull(tokenGenerator, "tokenGenerator cannot be null");
        this.authorizationService = authorizationService;
        this.tokenGenerator = tokenGenerator;
        this.authenticationManager = authenticationManager;
    }

    private static OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(Authentication authentication) {
        OAuth2ClientAuthenticationToken clientPrincipal = null;
        if (OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication.getPrincipal().getClass())) {
            clientPrincipal = (OAuth2ClientAuthenticationToken) authentication.getPrincipal();
        }
        if (clientPrincipal != null && clientPrincipal.isAuthenticated()) {
            return clientPrincipal;
        }
        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Instant startTrafficDt = Instant.now();
        CustomRefreshTokenAuthenticationToken customRefreshTokenAuthenticationToken = (CustomRefreshTokenAuthenticationToken) authentication;
        OAuth2ClientAuthenticationToken clientPrincipal =
                getAuthenticatedClientElseThrowInvalidClient(customRefreshTokenAuthenticationToken);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        // === customized area: take back the [refreshToken]
        String refreshToken = customRefreshTokenAuthenticationToken.getRefreshToken();
        OAuth2Authorization authorization = this.authorizationService.findByToken(refreshToken, OAuth2TokenType.REFRESH_TOKEN);
        assert registeredClient != null;
        if (authorization == null) {
            OAuth2Error oAuth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "Something went wrong with client [%s] authentication failed.".formatted(registeredClient.getId()), "NA");
            String message = "REFRESH_TOKEN is gone in storage.";
            throw new OAuth2AuthenticationException(oAuth2Error, message);
        }
        OAuth2Authorization.Token<OAuth2RefreshToken> oAuth2RefreshTokenToken = authorization.getRefreshToken();
        assert oAuth2RefreshTokenToken != null;
        if (!oAuth2RefreshTokenToken.isActive()) {
            OAuth2Error oAuth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Something went wrong with client [%s] token issues.".formatted(registeredClient.getId()), "NA");
            String message = "RT Expired At: ".concat(InstantUtil.parse(Objects.requireNonNull(oAuth2RefreshTokenToken.getToken().getExpiresAt())));
            throw new OAuth2AuthenticationException(oAuth2Error, message);
        }

        // == verify scope
        Set<String> scopes = registeredClient.getScopes();
        String identifier = authorization.getAttribute("username");
        // === FIXME: hibernate will dead. for one to many relationship. [hibernate session lazy load issue]
        var auth = authenticationService.findValidRecordsByDynamicIdentifier(identifier);
        Long userId = auth.getUser().getId();
        DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(new CustomRefreshTokenAuthenticationToken(refreshToken, clientPrincipal, null))
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorization(authorization)
                .authorizedScopes(scopes)
                .authorizationGrantType(customRefreshTokenAuthenticationToken.getGrantType())
                .authorizationGrant(customRefreshTokenAuthenticationToken)
                .put("userId", auth.getUser().getId())
                .put("identifier", identifier)
                .put("extReferenceMap", getUserMetaData(auth.getUser()));
        // === FIXME: hibernate will dead. for one to many relationship. [hibernate session lazy load issue]
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.from(authorization);

        // ----- Access token -----
        OAuth2TokenContext tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN).build();
        OAuth2Token generatedAccessToken = this.tokenGenerator.generate(tokenContext);
        if (generatedAccessToken == null) {
            OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                    "The token generator failed to generate the access token.", ERROR_URI);
            throw new OAuth2AuthenticationException(error);
        }

        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                generatedAccessToken.getTokenValue(), generatedAccessToken.getIssuedAt(),
                generatedAccessToken.getExpiresAt(), tokenContext.getAuthorizedScopes());
        if (generatedAccessToken instanceof ClaimAccessor) {
            authorizationBuilder.token(accessToken, (metadata) -> {
                metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, ((ClaimAccessor) generatedAccessToken).getClaims());
                metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, false);
            });
        } else {
            authorizationBuilder.accessToken(accessToken);
        }

        // ----- Refresh token -----
        OAuth2RefreshToken currentRefreshToken = null;
        if (registeredClient.getAuthorizationGrantTypes().contains(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_REFRESH_TOKEN.getKey())) &&
                // Do not issue refresh token to public client
                !clientPrincipal.getClientAuthenticationMethod().equals(ClientAuthenticationMethod.NONE)) {

            tokenContext = tokenContextBuilder.tokenType(new OAuth2TokenType(GrantTypeExtension.CUSTOM_REFRESH_TOKEN.getKey())).build(); // ** key
            OAuth2Token generatedRefreshToken = this.tokenGenerator.generate(tokenContext);
            if (!(generatedRefreshToken instanceof OAuth2RefreshToken)) {
                OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, "The token generator failed to generate the refresh token.", ERROR_URI);
                throw new OAuth2AuthenticationException(error);
            }
            if (log.isTraceEnabled()) {
                log.trace("Generated refresh token");
            }
            currentRefreshToken = (OAuth2RefreshToken) generatedRefreshToken;
            authorizationBuilder.refreshToken(currentRefreshToken);
        }

        // ----- ID token -----
        OidcIdToken idToken;
        if (scopes.contains(OidcScopes.OPENID)) {
            // @formatter:off
            tokenContext = tokenContextBuilder
                    .tokenType(new OAuth2TokenType(OidcParameterNames.ID_TOKEN))
                    .authorization(authorizationBuilder.build())	// ID token customizer may need access to the access token and/or refresh token
                    .build();
            // @formatter:on
            OAuth2Token generatedIdToken = this.tokenGenerator.generate(tokenContext);
            if (!(generatedIdToken instanceof org.springframework.security.oauth2.jwt.Jwt)) {
                OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                        "The token generator failed to generate the ID token.", ERROR_URI);
                throw new OAuth2AuthenticationException(error);
            }

            idToken = new OidcIdToken(generatedIdToken.getTokenValue(), generatedIdToken.getIssuedAt(),
                    generatedIdToken.getExpiresAt(), ((Jwt) generatedIdToken).getClaims());
            authorizationBuilder.token(idToken, (metadata) ->
                    metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, idToken.getClaims()));
        } else {
            idToken = null;
        }

        authorization = authorizationBuilder.build();
        this.authorizationService.save(authorization);
        Map<String, Object> additionalParameters = Collections.emptyMap();
        if (idToken != null) {
            additionalParameters = new HashMap<>();
            additionalParameters.put(OidcParameterNames.ID_TOKEN, idToken.getTokenValue());
        }
        OAuth2AccessTokenAuthenticationToken token = new OAuth2AccessTokenAuthenticationToken(registeredClient, clientPrincipal, accessToken, currentRefreshToken, additionalParameters);
        Map tokenUserMap = JSONUtil.convertFromObject(authentication, Map.class);
        tokenUserMap.put("username", identifier); // refreshToken dont have username ==> need to add back ==>> BUG found
        super.post_login_event(userId, GrantTypeExtension.CUSTOM_REFRESH_TOKEN.getKey(), startTrafficDt, tokenUserMap, token);
        return token;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return CustomRefreshTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private Map<String, Object> getUserMetaData(User user) {
        try {
            if (user.getMetadata() != null) {
                if (user.getMetadata().getExtReferenceMap() != null) {
                    return user.getMetadata().getExtReferenceMap();
                }
            }
        } catch (LazyInitializationException e) {
            // === FIXME: hibernate will dead. for one to many relationship. [hibernate session lazy load issue]
            log.info("--- Fail to get user metadata. Reason: {}", e.getMessage());
        }
        return new HashMap<>();
    }
}
