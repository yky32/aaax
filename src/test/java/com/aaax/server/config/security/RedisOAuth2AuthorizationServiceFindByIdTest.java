package com.aaax.server.config.security;

import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.config.extension.GrantTypeExtension;
import com.aaax.server.config.security.jwt.Jwt;
import com.aaax.server.config.security.jwt.JwtMetadata;
import com.aaax.server.config.security.jwt.JwtPayload;
import com.aaax.server.config.security.jwt.RegisteredClientMetadata;
import com.aaax.server.repository.UserTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisOAuth2AuthorizationServiceFindByIdTest {

    @Mock private RedisUtil redisUtil;
    @Mock private RegisteredClientRepository registeredClientRepository;
    @Mock private UserTokenRepository userTokenRepository;

    @InjectMocks
    private RedisOAuth2AuthorizationService service;

    private RegisteredClient registeredClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshTokenHistoryCount", 5);
        ReflectionTestUtils.setField(service, "serverTokenExpiryTime", 3600);
        registeredClient = RegisteredClient.withId("rc-1")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey()))
                .scope("openid")
                .build();
    }

    private Jwt sampleJwt() {
        Instant now = Instant.now();
        return Jwt.builder()
                .principalName("user@test.com")
                .accessToken("access")
                .accessTokenIssuedAt(now)
                .accessTokenExpiresAt(now.plusSeconds(60))
                .refreshToken("refresh")
                .refreshTokenIssuedAt(now)
                .refreshTokenExpiresAt(now.plusSeconds(120))
                .idToken("id")
                .authorizationGrantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey())
                .scopes(Set.of("openid"))
                .expiresIn(now.plusSeconds(60).getEpochSecond())
                .registeredClientMetadata(RegisteredClientMetadata.builder().id("rc-1").build())
                .payload(JwtPayload.builder()
                        .sub("10")
                        .metadata(JwtMetadata.builder().sessionId("sess-1").identifier("user@test.com").build())
                        .build())
                .build();
    }

    @Test
    @DisplayName("findById should transform redis jwt into authorization")
    void findById_shouldTransformJwt() {
        when(redisUtil.getOrElseThrow(anyString())).thenReturn(sampleJwt());
        when(registeredClientRepository.findById("rc-1")).thenReturn(registeredClient);

        OAuth2Authorization authorization = service.findById("10");

        assertNotNull(authorization);
        assertEquals("user@test.com", authorization.getPrincipalName());
        assertEquals("access", authorization.getAccessToken().getToken().getTokenValue());
        assertEquals("refresh", authorization.getRefreshToken().getToken().getTokenValue());
    }

    @Test
    @DisplayName("findByToken should transform redis jwt on hit")
    void findByToken_shouldTransformJwt() {
        when(redisUtil.getOrElseThrow(anyString())).thenReturn(sampleJwt());
        when(registeredClientRepository.findById("rc-1")).thenReturn(registeredClient);

        OAuth2Authorization authorization = service.findByToken("refresh", OAuth2TokenType.REFRESH_TOKEN);

        assertNotNull(authorization);
        assertEquals("refresh", authorization.getRefreshToken().getToken().getTokenValue());
    }

    @Test
    @DisplayName("remove should delete redis key using jwt subject")
    void remove_shouldDeleteUserTokenKey() {
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id("10")
                .principalName("user@test.com")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey()))
                .accessToken(new org.springframework.security.oauth2.core.OAuth2AccessToken(
                        org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER,
                        "access", Instant.now(), Instant.now().plusSeconds(60), Set.of("openid")))
                .refreshToken(new org.springframework.security.oauth2.core.OAuth2RefreshToken(
                        "refresh", Instant.now(), Instant.now().plusSeconds(120)))
                .token(new org.springframework.security.oauth2.core.oidc.OidcIdToken(
                        "id", Instant.now(), Instant.now().plusSeconds(60),
                        java.util.Map.of("sub", "10")))
                .build();

        // Claims required by convertAuthorizationToJwtClass
        // Access token claims may be null - convert uses accessToken.getClaims()
        // Need claims on access token metadata. OAuth2Authorization.token with claims:
        authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id("10")
                .principalName("user@test.com")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey()))
                .token(new org.springframework.security.oauth2.core.OAuth2AccessToken(
                                org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER,
                                "access", Instant.now(), Instant.now().plusSeconds(60), Set.of("openid")),
                        metadata -> metadata.put(
                                OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
                                JSONUtil.convertFromObject(JwtPayload.builder().sub("10").build(), java.util.Map.class)))
                .refreshToken(new org.springframework.security.oauth2.core.OAuth2RefreshToken(
                        "refresh", Instant.now(), Instant.now().plusSeconds(120)))
                .token(new org.springframework.security.oauth2.core.oidc.OidcIdToken(
                        "id", Instant.now(), Instant.now().plusSeconds(60),
                        java.util.Map.of("sub", "10")))
                .build();

        service.remove(authorization);
        verify(redisUtil).delete(contains("10"));
    }
}
