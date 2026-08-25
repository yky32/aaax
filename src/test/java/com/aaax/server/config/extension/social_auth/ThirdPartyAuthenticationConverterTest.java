package com.aaax.server.config.extension.social_auth;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThirdPartyAuthenticationConverterTest {

    @Mock private HttpServletRequest request;
    @Mock private OAuth2ClientAuthenticationToken clientPrincipal;

    private final ThirdPartyAuthenticationConverter converter = new ThirdPartyAuthenticationConverter();

    @Test
    @DisplayName("convert should return null for unrelated grant type")
    void convert_shouldReturnNullForOtherGrant() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE)).thenReturn("authorization_code");
        assertNull(converter.convert(request));
    }

    @Test
    @DisplayName("convert should build token for third-party grant")
    void convert_shouldBuildToken() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(GrantTypeExtension.THIRD_PARTY_OAUTH_GRANT.getKey());
        when(request.getParameterMap()).thenReturn(Map.of(
                OAuth2ParameterNames.GRANT_TYPE, new String[]{GrantTypeExtension.THIRD_PARTY_OAUTH_GRANT.getKey()},
                "idToken", new String[]{"id-token"},
                "provider", new String[]{"GOOGLE"},
                "deviceType", new String[]{"IOS"}
        ));
        SecurityContextHolder.getContext().setAuthentication(clientPrincipal);

        Authentication result = converter.convert(request);

        assertInstanceOf(ThirdPartyAuthenticationToken.class, result);
        ThirdPartyAuthenticationToken token = (ThirdPartyAuthenticationToken) result;
        assertEquals("id-token", token.getIdToken());
        assertEquals("GOOGLE", token.getProvider());
        assertEquals("IOS", token.getDeviceType());
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("convert should throw when required fields missing")
    void convert_shouldThrowWhenFieldsMissing() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(GrantTypeExtension.THIRD_PARTY_OAUTH_GRANT.getKey());
        when(request.getParameterMap()).thenReturn(Map.of(
                OAuth2ParameterNames.GRANT_TYPE, new String[]{GrantTypeExtension.THIRD_PARTY_OAUTH_GRANT.getKey()},
                "idToken", new String[]{"id-token"}
        ));
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));
    }
}
