package com.aaax.server.config.extension.qrcode;

import com.aaax.server.config.extension.GrantTypeExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrCodeGrantAuthenticationProviderHappyPathTest {

    @Mock private OAuth2AuthorizationService authorizationService;
    @Mock private OAuth2TokenGenerator<?> tokenGenerator;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private OAuth2ClientAuthenticationToken clientPrincipal;

    @Test
    @DisplayName("authenticate must not mint tokens for stub code 123")
    void authenticate_shouldRejectUnimplementedGrant() {
        var provider = new QrCodeGrantAuthenticationProvider(authorizationService, tokenGenerator, authenticationManager);
        var registeredClient = RegisteredClient.withId("rc-1")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.QR_CODE_GRANT.getKey()))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .scope("openid")
                .build();
        when(clientPrincipal.isAuthenticated()).thenReturn(true);
        when(clientPrincipal.getRegisteredClient()).thenReturn(registeredClient);

        QrCodeGrantAuthenticationToken auth =
                new QrCodeGrantAuthenticationToken("123", clientPrincipal, null);
        OAuth2AuthenticationException ex =
                assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(auth));
        assertEquals(OAuth2ErrorCodes.UNSUPPORTED_GRANT_TYPE, ex.getError().getErrorCode());
        verify(authorizationService, never()).save(any());
        verifyNoInteractions(tokenGenerator);
    }
}
