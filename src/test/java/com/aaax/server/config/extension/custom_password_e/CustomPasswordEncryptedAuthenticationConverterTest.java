package com.aaax.server.config.extension.custom_password_e;

import com.aaax.server.config.extension.GrantTypeExtension;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomPasswordEncryptedAuthenticationConverterTest {

    @Mock private HttpServletRequest request;
    @Mock private OAuth2ClientAuthenticationToken clientPrincipal;

    private final CustomPasswordEncryptedAuthenticationConverter converter =
            new CustomPasswordEncryptedAuthenticationConverter();

    @Test
    @DisplayName("convert should return null for unrelated grant")
    void convert_shouldReturnNull() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE)).thenReturn("authorization_code");
        assertNull(converter.convert(request));
    }

    @Test
    @DisplayName("convert should build encrypted password token")
    void convert_shouldBuildToken() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(GrantTypeExtension.CUSTOM_PASSWORD_GRANT_ENCRYPTED.getKey());
        when(request.getParameterMap()).thenReturn(Map.of(
                OAuth2ParameterNames.GRANT_TYPE, new String[]{GrantTypeExtension.CUSTOM_PASSWORD_GRANT_ENCRYPTED.getKey()},
                "username", new String[]{"user@test.com"},
                "credentials", new String[]{"c2VjcmV0"}
        ));
        SecurityContextHolder.getContext().setAuthentication(clientPrincipal);
        try {
            Authentication result = converter.convert(request);
            assertInstanceOf(CustomPasswordEncryptedAuthenticationToken.class, result);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("convert should throw when credentials missing")
    void convert_shouldThrowWhenMissing() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(GrantTypeExtension.CUSTOM_PASSWORD_GRANT_ENCRYPTED.getKey());
        when(request.getParameterMap()).thenReturn(Map.of(
                OAuth2ParameterNames.GRANT_TYPE, new String[]{GrantTypeExtension.CUSTOM_PASSWORD_GRANT_ENCRYPTED.getKey()},
                "username", new String[]{"user@test.com"}
        ));
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));
    }
}
