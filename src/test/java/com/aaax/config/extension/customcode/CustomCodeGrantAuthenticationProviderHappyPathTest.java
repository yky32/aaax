package com.aaax.config.extension.customcode;

import com.aaax.config.extension.GrantTypeExtension;
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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomCodeGrantAuthenticationProviderHappyPathTest {

    @Mock private OAuth2AuthorizationService authorizationService;
    @Mock private OAuth2TokenGenerator<?> tokenGenerator;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private OAuth2ClientAuthenticationToken clientPrincipal;

    private CustomCodeGrantAuthenticationProvider provider;
    private RegisteredClient registeredClient;

    @BeforeEach
    void setUp() {
        provider = new CustomCodeGrantAuthenticationProvider(authorizationService, tokenGenerator, authenticationManager);
        registeredClient = RegisteredClient.withId("rc-1")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_CODE_GRANT.getKey()))
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
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
    @DisplayName("authenticate should mint access, refresh, and id tokens")
    void authenticate_shouldMintTokens() {
        when(clientPrincipal.isAuthenticated()).thenReturn(true);
        when(clientPrincipal.getRegisteredClient()).thenReturn(registeredClient);
        when(clientPrincipal.getClientAuthenticationMethod()).thenReturn(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        when(clientPrincipal.getName()).thenReturn("client");

        Instant now = Instant.now();
        Jwt accessJwt = Jwt.withTokenValue("access-jwt")
                .header("alg", "RS256").issuedAt(now).expiresAt(now.plusSeconds(300)).claim("sub", "user").build();
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("refresh", now, now.plusSeconds(3600));
        Jwt idJwt = Jwt.withTokenValue("id-jwt")
                .header("alg", "RS256").issuedAt(now).expiresAt(now.plusSeconds(300)).claim("sub", "user").build();
        when(tokenGenerator.generate(any(OAuth2TokenContext.class))).thenAnswer(inv -> {
            OAuth2TokenContext ctx = inv.getArgument(0);
            if (OAuth2TokenType.ACCESS_TOKEN.equals(ctx.getTokenType())) return accessJwt;
            if (OAuth2TokenType.REFRESH_TOKEN.equals(ctx.getTokenType())) return refreshToken;
            if (OidcParameterNames.ID_TOKEN.equals(ctx.getTokenType().getValue())) return idJwt;
            return null;
        });

        CustomCodeGrantAuthenticationToken auth =
                new CustomCodeGrantAuthenticationToken("code-123", clientPrincipal, null);
        assertInstanceOf(OAuth2AccessTokenAuthenticationToken.class, provider.authenticate(auth));
        verify(authorizationService).save(any());
    }
}
