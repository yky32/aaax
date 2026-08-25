package com.aaax.server.config.extension.custom_password;

import com.aaax.server.config.extension.GrantTypeExtension;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.util.Assert;

import java.util.Map;

public class CustomPasswordAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {
    private final String username;
    private final String credentials;

    public CustomPasswordAuthenticationToken(String username, String password, Authentication clientPrincipal,
                                             @Nullable Map<String, Object> additionalParameters) {
        super(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey()),
                clientPrincipal, additionalParameters);
        Assert.hasText(username, "username cannot be empty");
        Assert.hasText(password, "password cannot be empty");

        this.username = username;
        this.credentials = password;
    }

    public String getUsername() {
        return this.username;
    }

    public String getCredentials() {
        return this.credentials;
    }

    @Override
    public String getName() {
        return this.username;
    }
}
