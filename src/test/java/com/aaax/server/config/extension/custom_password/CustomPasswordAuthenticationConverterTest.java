package com.aaax.server.config.extension.custom_password;

import com.aaax.server.config.extension.GrantTypeExtension;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomPasswordAuthenticationConverterTest {

    @Mock private HttpServletRequest request;
    @Mock private OAuth2ClientAuthenticationToken clientPrincipal;

    private final CustomPasswordAuthenticationConverter converter = new CustomPasswordAuthenticationConverter();

    @Test
    @DisplayName("convert should return null for unrelated grant type")
    void convert_shouldReturnNullForOtherGrant() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE)).thenReturn("authorization_code");
        assertNull(converter.convert(request));
    }

    @Test
    @DisplayName("convert should build token for custom password grant")
    void convert_shouldBuildToken() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey());
        when(request.getParameterMap()).thenReturn(Map.of(
                OAuth2ParameterNames.GRANT_TYPE, new String[]{GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey()},
                "username", new String[]{"user@test.com"},
                "credentials", new String[]{"secret"}
        ));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(clientPrincipal);

        Authentication result = converter.convert(request);

        assertInstanceOf(CustomPasswordAuthenticationToken.class, result);
        CustomPasswordAuthenticationToken token = (CustomPasswordAuthenticationToken) result;
        assertEquals("user@test.com", token.getUsername());
        assertEquals("secret", token.getCredentials());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("convert should throw when username missing")
    void convert_shouldThrowWhenUsernameMissing() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey());
        when(request.getParameterMap()).thenReturn(Map.of(
                OAuth2ParameterNames.GRANT_TYPE, new String[]{GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey()},
                "credentials", new String[]{"secret"}
        ));
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));
    }
}
