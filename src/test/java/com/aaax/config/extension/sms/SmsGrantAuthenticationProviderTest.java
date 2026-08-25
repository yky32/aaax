package com.aaax.config.extension.sms;

import com.aaax.config.extension.GrantTypeExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsGrantAuthenticationProviderTest {

    @Mock private OAuth2AuthorizationService authorizationService;
    @Mock private OAuth2TokenGenerator<?> tokenGenerator;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private OAuth2ClientAuthenticationToken clientPrincipal;

    private SmsGrantAuthenticationProvider provider;
    private RegisteredClient registeredClient;

    @BeforeEach
    void setUp() {
        provider = new SmsGrantAuthenticationProvider(authorizationService, tokenGenerator, authenticationManager);
        registeredClient = RegisteredClient.withId("rc-1")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.SMS_GRANT.getKey()))
                .scope("openid")
                .build();
    }

    @Test
    @DisplayName("supports should accept SmsGrantAuthenticationToken")
    void supports_shouldAcceptToken() {
        assertTrue(provider.supports(SmsGrantAuthenticationToken.class));
    }

    @Test
    @DisplayName("authenticate should reject invalid client")
    void authenticate_shouldRejectInvalidClient() {
        when(clientPrincipal.isAuthenticated()).thenReturn(false);
        SmsGrantAuthenticationToken auth = new SmsGrantAuthenticationToken("123", clientPrincipal, null);
        assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(auth));
    }

    @Test
    @DisplayName("authenticate should throw when token generator returns null")
    void authenticate_shouldThrowWhenTokenGeneratorFails() {
        when(clientPrincipal.isAuthenticated()).thenReturn(true);
        when(clientPrincipal.getRegisteredClient()).thenReturn(registeredClient);
        when(tokenGenerator.generate(any())).thenReturn(null);
        AuthorizationServerContext serverContext = mock(AuthorizationServerContext.class);
        AuthorizationServerContextHolder.setContext(serverContext);
        try {
            SmsGrantAuthenticationToken auth = new SmsGrantAuthenticationToken("123", clientPrincipal, null);
            assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(auth));
        } finally {
            AuthorizationServerContextHolder.resetContext();
        }
    }
}
