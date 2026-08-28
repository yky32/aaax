package com.aaax.server.config.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RfcOAuth2TokenHttpTest {

    @Test
    @DisplayName("success body should use RFC access_token fields")
    void writeSuccess_shouldUseRfcNames() throws Exception {
        Instant now = Instant.now();
        OAuth2AccessToken access = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "at-1", now, now.plusSeconds(60), Set.of("openid"));
        OAuth2RefreshToken refresh = new OAuth2RefreshToken("rt-1", now, now.plusSeconds(120));
        RegisteredClient client = RegisteredClient.withId("id")
                .clientId("client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientSettings(ClientSettings.builder().build())
                .build();
        OAuth2ClientAuthenticationToken principal = new OAuth2ClientAuthenticationToken(
                client, ClientAuthenticationMethod.CLIENT_SECRET_BASIC, "secret");
        OAuth2AccessTokenAuthenticationToken token =
                new OAuth2AccessTokenAuthenticationToken(client, principal, access, refresh);

        MockHttpServletResponse response = new MockHttpServletResponse();
        RfcOAuth2TokenHttp.writeSuccess(response, token);
        String body = response.getContentAsString();
        assertTrue(body.contains("\"access_token\":\"at-1\""));
        assertTrue(body.contains("\"refresh_token\":\"rt-1\""));
        assertTrue(body.contains("\"token_type\":\"Bearer\""));
        assertTrue(body.contains("\"expires_in\""));
        assertFalse(body.contains("accessToken"));
        assertFalse(body.contains("\"data\""));
    }

    @Test
    @DisplayName("error body should use RFC error fields")
    void writeError_shouldUseRfcNames() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        RfcOAuth2TokenHttp.writeError(response, new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "bad refresh", null)));
        String body = response.getContentAsString();
        assertTrue(body.contains("\"error\":\"invalid_grant\""));
        assertTrue(body.contains("\"error_description\":\"bad refresh\""));
        assertFalse(body.contains("\"code\""));
    }
}
