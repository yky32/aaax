package com.aaax.server.config.extension;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2TokenGeneratorTest {

    @Mock private JwtEncoder jwtEncoder;
    @Mock private HttpServletRequest request;
    @Mock private OAuth2TokenContext context;

    private CustomOAuth2TokenGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new CustomOAuth2TokenGenerator(jwtEncoder, request);
    }

    @Test
    @DisplayName("generate should return null when token type unsupported")
    void generate_shouldReturnNullForUnsupportedType() {
        when(context.getTokenType()).thenReturn(OAuth2TokenType.REFRESH_TOKEN);
        assertNull(generator.generate(context));
    }

    @Test
    @DisplayName("generate should return null for non self-contained access token format")
    void generate_shouldReturnNullForReferenceFormat() {
        RegisteredClient client = RegisteredClient.withId("rc")
                .clientId("c")
                .clientSecret("s")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .build())
                .build();
        when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
        when(context.getRegisteredClient()).thenReturn(client);
        assertNull(generator.generate(context));
    }

    @Test
    @DisplayName("generate should encode JWT for user access token")
    void generate_shouldEncodeUserAccessJwt() {
        RegisteredClient client = RegisteredClient.withId("rc")
                .clientId("client-id")
                .clientSecret("s")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey()))
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .build())
                .build();
        when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
        when(context.getRegisteredClient()).thenReturn(client);
        when(context.getAuthorizationServerContext()).thenReturn(null);
        when(context.getAuthorizationGrantType())
                .thenReturn(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey()));
        when(context.getAuthorizedScopes()).thenReturn(Set.of("openid"));
        when(context.get("userId")).thenReturn("42");
        when(context.get("identifier")).thenReturn("user@test.com");
        when(context.get("extReferenceMap")).thenReturn(Map.of("k", "v"));
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("aaax.test");

        Jwt jwt = Jwt.withTokenValue("user-jwt")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .subject("42")
                .build();
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        assertEquals(jwt, generator.generate(context));
    }

    @Test
    @DisplayName("generate should use UserPrincipal when password-grant context keys are absent")
    void generate_shouldUseUserPrincipalForAuthorizationCode() {
        RegisteredClient client = RegisteredClient.withId("rc")
                .clientId("aaax-pkce")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.NONE)
                .redirectUri("http://127.0.0.1:9/authorized")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .build())
                .build();
        com.aaax.server.entity.po.user.UserPrincipal user =
                new com.aaax.server.entity.po.user.UserPrincipal(
                        42L, "smoke.primary@aaax.local", "x", java.util.List.of());
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken login =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        user, "x", user.getAuthorities());
        when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
        when(context.getRegisteredClient()).thenReturn(client);
        when(context.getAuthorizationServerContext()).thenReturn(null);
        when(context.getAuthorizationGrantType()).thenReturn(AuthorizationGrantType.AUTHORIZATION_CODE);
        when(context.getAuthorizedScopes()).thenReturn(Set.of("openid"));
        when(context.get("userId")).thenReturn(null);
        when(context.get("identifier")).thenReturn(null);
        when(context.getPrincipal()).thenReturn(login);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");

        Jwt jwt = Jwt.withTokenValue("pkce-jwt")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .subject("42")
                .build();
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        assertEquals(jwt, generator.generate(context));
    }
}
