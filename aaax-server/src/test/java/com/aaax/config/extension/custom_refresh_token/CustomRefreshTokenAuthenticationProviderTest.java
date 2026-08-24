package com.aaax.config.extension.custom_refresh_token;

import com.aaax.config.extension.GrantTypeExtension;
import com.aaax.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomRefreshTokenAuthenticationProviderTest {

    @Mock private OAuth2AuthorizationService authorizationService;
    @Mock private OAuth2TokenGenerator<?> tokenGenerator;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AuthenticationService authenticationService;
    @Mock private OAuth2ClientAuthenticationToken clientPrincipal;

    private CustomRefreshTokenAuthenticationProvider provider;
    private RegisteredClient registeredClient;

    @BeforeEach
    void setUp() {
        provider = new CustomRefreshTokenAuthenticationProvider(authorizationService, tokenGenerator, authenticationManager);
        ReflectionTestUtils.setField(provider, "authenticationService", authenticationService);
        registeredClient = RegisteredClient.withId("rc-1")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_REFRESH_TOKEN.getKey()))
                .build();
    }

    @Test
    @DisplayName("supports should accept CustomRefreshTokenAuthenticationToken")
    void supports_shouldAcceptToken() {
        assertTrue(provider.supports(CustomRefreshTokenAuthenticationToken.class));
    }

    @Test
    @DisplayName("authenticate should reject invalid client")
    void authenticate_shouldRejectInvalidClient() {
        when(clientPrincipal.isAuthenticated()).thenReturn(false);
        CustomRefreshTokenAuthenticationToken auth =
                new CustomRefreshTokenAuthenticationToken("rt", clientPrincipal, null);
        assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(auth));
    }

    @Test
    @DisplayName("authenticate should throw when refresh token missing from storage")
    void authenticate_shouldThrowWhenRefreshMissing() {
        when(clientPrincipal.isAuthenticated()).thenReturn(true);
        when(clientPrincipal.getRegisteredClient()).thenReturn(registeredClient);
        when(authorizationService.findByToken("rt-gone", OAuth2TokenType.REFRESH_TOKEN)).thenReturn(null);

        CustomRefreshTokenAuthenticationToken auth =
                new CustomRefreshTokenAuthenticationToken("rt-gone", clientPrincipal, null);

        OAuth2AuthenticationException ex =
                assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(auth));
        assertTrue(ex.getMessage().contains("REFRESH_TOKEN is gone") || ex.getError() != null);
    }

    @Test
    @DisplayName("authenticate should throw when refresh token expired")
    void authenticate_shouldThrowWhenRefreshExpired() {
        when(clientPrincipal.isAuthenticated()).thenReturn(true);
        when(clientPrincipal.getRegisteredClient()).thenReturn(registeredClient);

        OAuth2RefreshToken expired = new OAuth2RefreshToken(
                "rt-old", Instant.now().minusSeconds(120), Instant.now().minusSeconds(60));
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id("1")
                .principalName("user@test.com")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_REFRESH_TOKEN.getKey()))
                .refreshToken(expired)
                .attribute("username", "user@test.com")
                .build();
        when(authorizationService.findByToken(eq("rt-old"), eq(OAuth2TokenType.REFRESH_TOKEN)))
                .thenReturn(authorization);

        CustomRefreshTokenAuthenticationToken auth =
                new CustomRefreshTokenAuthenticationToken("rt-old", clientPrincipal, null);

        assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(auth));
    }
}
