package com.aaax.server.config.extension.custom_password;

import com.aaax.server.config.extension.custom_password_e.CustomPasswordEncryptedAuthenticationProvider;
import com.aaax.server.config.extension.custom_password_e.CustomPasswordEncryptedAuthenticationToken;
import com.aaax.server.config.extension.custom_refresh_token.CustomRefreshTokenAuthenticationProvider;
import com.aaax.server.config.extension.custom_refresh_token.CustomRefreshTokenAuthenticationToken;
import com.aaax.server.config.extension.customcode.CustomCodeGrantAuthenticationProvider;
import com.aaax.server.config.extension.customcode.CustomCodeGrantAuthenticationToken;
import com.aaax.server.config.extension.qrcode.QrCodeGrantAuthenticationProvider;
import com.aaax.server.config.extension.qrcode.QrCodeGrantAuthenticationToken;
import com.aaax.server.config.extension.sms.SmsGrantAuthenticationProvider;
import com.aaax.server.config.extension.sms.SmsGrantAuthenticationToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationProvidersSupportsTest {

    @Mock private OAuth2AuthorizationService authorizationService;
    @Mock private OAuth2TokenGenerator<?> tokenGenerator;
    @Mock private AuthenticationManager authenticationManager;

    @Test
    @DisplayName("custom password provider should support its token type")
    void customPassword_shouldSupportToken() {
        CustomPasswordAuthenticationProvider provider =
                new CustomPasswordAuthenticationProvider(authorizationService, tokenGenerator, authenticationManager);
        assertTrue(provider.supports(CustomPasswordAuthenticationToken.class));
        assertFalse(provider.supports(String.class));
    }

    @Test
    @DisplayName("encrypted password provider should support its token type")
    void encryptedPassword_shouldSupportToken() {
        CustomPasswordEncryptedAuthenticationProvider provider =
                new CustomPasswordEncryptedAuthenticationProvider(authorizationService, tokenGenerator, authenticationManager);
        assertTrue(provider.supports(CustomPasswordEncryptedAuthenticationToken.class));
        assertFalse(provider.supports(CustomPasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("refresh token provider should support its token type")
    void refreshToken_shouldSupportToken() {
        CustomRefreshTokenAuthenticationProvider provider =
                new CustomRefreshTokenAuthenticationProvider(authorizationService, tokenGenerator, authenticationManager);
        assertTrue(provider.supports(CustomRefreshTokenAuthenticationToken.class));
    }

    @Test
    @DisplayName("sms qrcode and customcode providers should support their tokens")
    void otherProviders_shouldSupportTokens() {
        assertTrue(new SmsGrantAuthenticationProvider(authorizationService, tokenGenerator, authenticationManager)
                .supports(SmsGrantAuthenticationToken.class));
        assertTrue(new QrCodeGrantAuthenticationProvider(authorizationService, tokenGenerator, authenticationManager)
                .supports(QrCodeGrantAuthenticationToken.class));
        assertTrue(new CustomCodeGrantAuthenticationProvider(authorizationService, tokenGenerator, authenticationManager)
                .supports(CustomCodeGrantAuthenticationToken.class));
    }
}
