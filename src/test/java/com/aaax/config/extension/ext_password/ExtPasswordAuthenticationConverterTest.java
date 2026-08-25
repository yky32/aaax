package com.aaax.config.extension.ext_password;

import com.aaax.config.extension.GrantTypeExtension;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExtPasswordAuthenticationConverterTest {

    @Mock private HttpServletRequest request;
    @Mock private OAuth2ClientAuthenticationToken clientPrincipal;

    private final ExtPasswordAuthenticationConverter converter = new ExtPasswordAuthenticationConverter();

    @Test
    @DisplayName("convert should return null for unrelated grant type")
    void convert_shouldReturnNullForOtherGrant() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE)).thenReturn("authorization_code");
        assertNull(converter.convert(request));
    }

    @Test
    @DisplayName("convert should build token for ext password grant")
    void convert_shouldBuildToken() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(GrantTypeExtension.EXT_PASSWORD_GRANT.getKey());
        when(request.getParameterMap()).thenReturn(Map.of(
                OAuth2ParameterNames.GRANT_TYPE, new String[]{GrantTypeExtension.EXT_PASSWORD_GRANT.getKey()},
                "username", new String[]{"user@test.com"},
                "credentials", new String[]{"secret"}
        ));
        SecurityContextHolder.getContext().setAuthentication(clientPrincipal);

        Authentication result = converter.convert(request);

        assertInstanceOf(ExtPasswordAuthenticationToken.class, result);
        ExtPasswordAuthenticationToken token = (ExtPasswordAuthenticationToken) result;
        assertEquals("user@test.com", token.getUsername());
        assertEquals("secret", token.getCredentials());
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("convert should throw when credentials missing")
    void convert_shouldThrowWhenCredentialsMissing() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(GrantTypeExtension.EXT_PASSWORD_GRANT.getKey());
        when(request.getParameterMap()).thenReturn(Map.of(
                OAuth2ParameterNames.GRANT_TYPE, new String[]{GrantTypeExtension.EXT_PASSWORD_GRANT.getKey()},
                "username", new String[]{"user@test.com"}
        ));
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));
    }
}
