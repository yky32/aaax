package com.aaax.config.extension.sms;

import com.aaax.config.extension.GrantTypeExtension;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.util.Assert;

import java.util.Map;

public class SmsGrantAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {
    private final String code;

    public SmsGrantAuthenticationToken(String code, Authentication clientPrincipal,
                                       @Nullable Map<String, Object> additionalParameters) {
        super(new AuthorizationGrantType(GrantTypeExtension.SMS_GRANT.getKey()),
                clientPrincipal, additionalParameters);
        Assert.hasText(code, "code cannot be empty");
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

}
