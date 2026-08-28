package com.aaax.server.config.extension;

import com.aaax.core.utils.JSONUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** RFC 6749 §5 token endpoint JSON (no AAAX Result envelope). */
public final class RfcOAuth2TokenHttp {

    private RfcOAuth2TokenHttp() {
    }

    @SneakyThrows
    public static void writeSuccess(HttpServletResponse response, OAuth2AccessTokenAuthenticationToken token) {
        OAuth2AccessToken access = token.getAccessToken();
        long expiresIn = 0;
        Instant expiresAt = access.getExpiresAt();
        if (expiresAt != null) {
            expiresIn = Math.max(0, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", access.getTokenValue());
        body.put("token_type", OAuth2AccessToken.TokenType.BEARER.getValue());
        body.put("expires_in", expiresIn);
        Set<String> scopes = access.getScopes();
        if (scopes != null && !scopes.isEmpty()) {
            body.put("scope", scopes.stream().collect(Collectors.joining(" ")));
        }
        OAuth2RefreshToken refresh = token.getRefreshToken();
        if (refresh != null && refresh.getTokenValue() != null && !refresh.getTokenValue().isBlank()
                && !"--NA".equals(refresh.getTokenValue())) {
            body.put("refresh_token", refresh.getTokenValue());
        }
        write(response, HttpStatus.OK.value(), body);
    }

    @SneakyThrows
    public static void writeError(HttpServletResponse response, Exception exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (exception instanceof OAuth2AuthenticationException oauth) {
            OAuth2Error error = oauth.getError();
            body.put("error", error.getErrorCode());
            if (error.getDescription() != null && !error.getDescription().isBlank()) {
                body.put("error_description", error.getDescription());
            }
        } else {
            body.put("error", "invalid_request");
            body.put("error_description", Objects.toString(exception.getMessage(), "token request failed"));
        }
        write(response, HttpStatus.BAD_REQUEST.value(), body);
    }

    @SneakyThrows
    static void write(HttpServletResponse response, int status, Map<String, Object> body) {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JSONUtil.writeValue(body));
    }
}
