package com.aaax.server.config.extension;

import com.aaax.core.utils.JSONUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** RFC 6749 §5 token endpoint JSON. Java fields are camelCase; wire names are snake_case. */
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
        Set<String> scopes = access.getScopes();
        String scope = (scopes == null || scopes.isEmpty())
                ? null
                : scopes.stream().collect(Collectors.joining(" "));
        String refreshToken = null;
        OAuth2RefreshToken refresh = token.getRefreshToken();
        if (refresh != null && refresh.getTokenValue() != null && !refresh.getTokenValue().isBlank()) {
            refreshToken = refresh.getTokenValue();
        }
        write(response, HttpStatus.OK.value(), new TokenResponse(
                access.getTokenValue(),
                OAuth2AccessToken.TokenType.BEARER.getValue(),
                expiresIn,
                scope,
                refreshToken
        ));
    }

    @SneakyThrows
    public static void writeError(HttpServletResponse response, Exception exception) {
        String error = "invalid_request";
        String errorDescription = Objects.toString(exception.getMessage(), "token request failed");
        if (exception instanceof OAuth2AuthenticationException oauth) {
            OAuth2Error oauthError = oauth.getError();
            error = oauthError.getErrorCode();
            errorDescription = (oauthError.getDescription() == null || oauthError.getDescription().isBlank())
                    ? null
                    : oauthError.getDescription();
        }
        write(response, HttpStatus.BAD_REQUEST.value(), new TokenError(error, errorDescription));
    }

    @SneakyThrows
    static void write(HttpServletResponse response, int status, Object body) {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JSONUtil.writeValue(body));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record TokenResponse(
            @JsonProperty(OAuth2ParameterNames.ACCESS_TOKEN) String accessToken,
            @JsonProperty(OAuth2ParameterNames.TOKEN_TYPE) String tokenType,
            @JsonProperty(OAuth2ParameterNames.EXPIRES_IN) long expiresIn,
            @JsonProperty(OAuth2ParameterNames.SCOPE) String scope,
            @JsonProperty(OAuth2ParameterNames.REFRESH_TOKEN) String refreshToken
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record TokenError(
            @JsonProperty(OAuth2ParameterNames.ERROR) String error,
            @JsonProperty(OAuth2ParameterNames.ERROR_DESCRIPTION) String errorDescription
    ) {
    }
}
