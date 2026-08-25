package com.aaax.config.extension.ext_password;

import com.aaax.config.extension.GrantTypeExtension;
import com.aaax.entity.po.user.Authentication;
import com.aaax.entity.po.user.User;
import com.aaax.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtPasswordAuthenticationProviderTest {

    @Mock private OAuth2AuthorizationService authorizationService;
    @Mock private OAuth2TokenGenerator<?> tokenGenerator;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AuthenticationService authenticationService;
    @Mock private OAuth2ClientAuthenticationToken clientPrincipal;

    private ExtPasswordAuthenticationProvider provider;
    private RegisteredClient registeredClient;

    @BeforeEach
    void setUp() {
        provider = new ExtPasswordAuthenticationProvider(authorizationService, tokenGenerator, authenticationManager);
        ReflectionTestUtils.setField(provider, "authenticationService", authenticationService);
        registeredClient = RegisteredClient.withId("rc-1")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.EXT_PASSWORD_GRANT.getKey()))
                .scope("openid")
                .build();
    }

    @Test
    @DisplayName("authenticate should quick-return existing redis authorization")
    void authenticate_shouldQuickReturnFromRedis() {
        when(clientPrincipal.isAuthenticated()).thenReturn(true);
        when(clientPrincipal.getRegisteredClient()).thenReturn(registeredClient);
        ExtPasswordAuthenticationToken auth =
                new ExtPasswordAuthenticationToken("user@test.com", "pass", clientPrincipal, null);

        Authentication userAuth = Authentication.builder()
                .identifier("user@test.com")
                .user(User.builder().id(9L).build())
                .build();
        when(authenticationService.findValidRecordsByDynamicIdentifier("user@test.com")).thenReturn(userAuth);

        Instant now = Instant.now();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access", now, now.plusSeconds(60));
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("refresh", now, now.plusSeconds(120));
        OidcIdToken idToken = new OidcIdToken("id-token", now, now.plusSeconds(60), Map.of("sub", "user@test.com"));
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id("9")
                .principalName("user@test.com")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.EXT_PASSWORD_GRANT.getKey()))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .token(idToken)
                .build();
        when(authorizationService.findById("9")).thenReturn(authorization);

        assertInstanceOf(OAuth2AccessTokenAuthenticationToken.class, provider.authenticate(auth));
        verify(authenticationService).post_check(userAuth, true);
    }

    @Test
    @DisplayName("supports should accept ExtPasswordAuthenticationToken")
    void supports_shouldAccept() {
        assertTrue(provider.supports(ExtPasswordAuthenticationToken.class));
    }
}
