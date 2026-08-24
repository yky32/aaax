package com.aaax.config.extension.custom_refresh_token;

import com.aaax.core.utils.KafkaUtil;
import com.aaax.config.extension.GrantTypeExtension;
import com.aaax.entity.po.user.Authentication;
import com.aaax.entity.po.user.User;
import com.aaax.service.AuthenticationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomRefreshTokenAuthenticationProviderHappyPathTest {

    @Mock private OAuth2AuthorizationService authorizationService;
    @Mock private OAuth2TokenGenerator<?> tokenGenerator;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AuthenticationService authenticationService;
    @Mock private KafkaUtil kafkaUtil;
    @Mock private OAuth2ClientAuthenticationToken clientPrincipal;

    private CustomRefreshTokenAuthenticationProvider provider;
    private RegisteredClient registeredClient;

    @BeforeEach
    void setUp() {
        provider = new CustomRefreshTokenAuthenticationProvider(authorizationService, tokenGenerator, authenticationManager);
        ReflectionTestUtils.setField(provider, "authenticationService", authenticationService);
        ReflectionTestUtils.setField(provider, "kafkaUtil", kafkaUtil);
        registeredClient = RegisteredClient.withId("rc-1")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_REFRESH_TOKEN.getKey()))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .scope("openid")
                .build();
        AuthorizationServerSettings settings = AuthorizationServerSettings.builder().issuer("https://uaa.test").build();
        AuthorizationServerContextHolder.setContext(new AuthorizationServerContext() {
            @Override public String getIssuer() { return settings.getIssuer(); }
            @Override public AuthorizationServerSettings getAuthorizationServerSettings() { return settings; }
        });
    }

    @AfterEach
    void tearDown() {
        AuthorizationServerContextHolder.resetContext();
    }

    @Test
    @DisplayName("authenticate should rotate tokens for active refresh token")
    void authenticate_shouldRotateTokens() {
        when(clientPrincipal.isAuthenticated()).thenReturn(true);
        when(clientPrincipal.getRegisteredClient()).thenReturn(registeredClient);
        when(clientPrincipal.getClientAuthenticationMethod()).thenReturn(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

        Instant now = Instant.now();
        OAuth2RefreshToken existingRt = new OAuth2RefreshToken("rt-old", now.minusSeconds(10), now.plusSeconds(3600));
        OAuth2AccessToken existingAt = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "old-access", now.minusSeconds(10), now.plusSeconds(60));
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id("1")
                .principalName("user@test.com")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_REFRESH_TOKEN.getKey()))
                .accessToken(existingAt)
                .refreshToken(existingRt)
                .attribute("username", "user@test.com")
                .build();
        when(authorizationService.findByToken("rt-old", OAuth2TokenType.REFRESH_TOKEN)).thenReturn(authorization);

        Authentication userAuth = Authentication.builder()
                .identifier("user@test.com")
                .user(User.builder().id(5L).username("user@test.com").build())
                .build();
        when(authenticationService.findValidRecordsByDynamicIdentifier("user@test.com")).thenReturn(userAuth);

        Jwt accessJwt = Jwt.withTokenValue("new-access")
                .header("alg", "RS256").issuedAt(now).expiresAt(now.plusSeconds(300)).claim("sub", "5").build();
        OAuth2RefreshToken newRt = new OAuth2RefreshToken("rt-new", now, now.plusSeconds(3600));
        Jwt idJwt = Jwt.withTokenValue("id-jwt")
                .header("alg", "RS256").issuedAt(now).expiresAt(now.plusSeconds(300)).claim("sub", "5").build();
        when(tokenGenerator.generate(any(OAuth2TokenContext.class))).thenAnswer(inv -> {
            OAuth2TokenContext ctx = inv.getArgument(0);
            if (OAuth2TokenType.ACCESS_TOKEN.equals(ctx.getTokenType())) return accessJwt;
            if (GrantTypeExtension.CUSTOM_REFRESH_TOKEN.getKey().equals(ctx.getTokenType().getValue())) return newRt;
            if (OidcParameterNames.ID_TOKEN.equals(ctx.getTokenType().getValue())) return idJwt;
            return null;
        });

        CustomRefreshTokenAuthenticationToken auth =
                new CustomRefreshTokenAuthenticationToken("rt-old", clientPrincipal, null);
        assertInstanceOf(OAuth2AccessTokenAuthenticationToken.class, provider.authenticate(auth));
        verify(authorizationService).save(any());
        verify(kafkaUtil).send(anyString(), any());
    }
}
