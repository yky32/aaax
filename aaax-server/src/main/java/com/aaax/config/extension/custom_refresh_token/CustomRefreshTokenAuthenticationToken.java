package com.aaax.config.extension.custom_refresh_token;

import com.aaax.config.extension.GrantTypeExtension;
import lombok.Getter;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.util.Assert;

import java.util.Map;

@Getter
public class CustomRefreshTokenAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    private final String refreshToken;

    public CustomRefreshTokenAuthenticationToken(String refreshToken, Authentication clientPrincipal,
                                                 @Nullable Map<String, Object> additionalParameters) {
        super(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_REFRESH_TOKEN.getKey()),
                clientPrincipal, additionalParameters);
        Assert.hasText(refreshToken, "refreshToken cannot be empty");
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }
}
