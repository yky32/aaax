package com.aaax.server.config.extension.qrcode;

import com.aaax.server.config.extension.GrantTypeExtension;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.util.Assert;

import java.util.Map;

public class QrCodeGrantAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {
    private final String refreshToken;

    public QrCodeGrantAuthenticationToken(String refreshToken, Authentication clientPrincipal,
                                          @Nullable Map<String, Object> additionalParameters) {
        super(new AuthorizationGrantType(GrantTypeExtension.QR_CODE_GRANT.getKey()),
                clientPrincipal, additionalParameters);
        Assert.hasText(refreshToken, "refreshToken cannot be empty");
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

}
