package com.aaax.config.extension.sms;

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
class SmsGrantAuthenticationConverterTest {

    @Mock private HttpServletRequest request;
    @Mock private OAuth2ClientAuthenticationToken clientPrincipal;

    private final SmsGrantAuthenticationConverter converter = new SmsGrantAuthenticationConverter();

    @Test
    @DisplayName("convert should return null for unrelated grant type")
    void convert_shouldReturnNullForOtherGrant() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE)).thenReturn("authorization_code");
        assertNull(converter.convert(request));
    }

    @Test
    @DisplayName("convert should build token for sms grant")
    void convert_shouldBuildToken() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(GrantTypeExtension.SMS_GRANT.getKey());
        when(request.getParameterMap()).thenReturn(Map.of(
                OAuth2ParameterNames.GRANT_TYPE, new String[]{GrantTypeExtension.SMS_GRANT.getKey()},
                OAuth2ParameterNames.CODE, new String[]{"123456"},
                "extra", new String[]{"v"}
        ));
        SecurityContextHolder.getContext().setAuthentication(clientPrincipal);

        Authentication result = converter.convert(request);

        assertInstanceOf(SmsGrantAuthenticationToken.class, result);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("convert should throw when code missing")
    void convert_shouldThrowWhenCodeMissing() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(GrantTypeExtension.SMS_GRANT.getKey());
        when(request.getParameterMap()).thenReturn(Map.of(
                OAuth2ParameterNames.GRANT_TYPE, new String[]{GrantTypeExtension.SMS_GRANT.getKey()}
        ));
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));
    }
}
