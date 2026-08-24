package com.aaax.config.extension.social_auth;

import com.aaax.config.extension.GrantTypeExtension;
import lombok.Getter;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.util.Assert;

import java.util.Map;

@Getter
public class ThirdPartyAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {
    private final String idToken;
    private final String provider;
    private final String deviceType;

    public ThirdPartyAuthenticationToken(String idToken, String provider, String deviceType, Authentication clientPrincipal,
                                         @Nullable Map<String, Object> additionalParameters) {
        super(new AuthorizationGrantType(GrantTypeExtension.THIRD_PARTY_OAUTH_GRANT.getKey()),
                clientPrincipal, additionalParameters);
        Assert.hasText(idToken, "username cannot be empty");
        Assert.hasText(provider, "password cannot be empty");
        Assert.hasText(deviceType, "password cannot be empty");


        this.idToken = idToken;
        this.provider = provider;
        this.deviceType = deviceType;
    }
}