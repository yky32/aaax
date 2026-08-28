package com.aaax.server.config.extension;

import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.Arrays;

public enum GrantTypeExtension {
    CUSTOM_PASSWORD_GRANT("custom-password-grant"),
    CUSTOM_PASSWORD_GRANT_ENCRYPTED("custom-password-grant:e"),
    CUSTOM_CODE_GRANT("urn:ietf:params:oauth:grant-type:custom_code"),
    QR_CODE_GRANT("urn:ietf:params:oauth:grant-type:qr_code"),
    SMS_GRANT("urn:ietf:params:oauth:grant-type:custom_code"),
    CUSTOM_REFRESH_TOKEN("refresh_token"),
    EXT_PASSWORD_GRANT("ext-password-grant"),
    THIRD_PARTY_OAUTH_GRANT("third-party-grant")
    ;

    private static final String LEGACY_REFRESH_GRANT = "refresh-token";

    private final String key;

    GrantTypeExtension(String key) {
        this.key = key;
    }

    public static boolean isRefreshGrant(String grantTypeKey) {
        return grantTypeKey != null && (
                CUSTOM_REFRESH_TOKEN.key.equalsIgnoreCase(grantTypeKey)
                        || LEGACY_REFRESH_GRANT.equalsIgnoreCase(grantTypeKey)
                        || AuthorizationGrantType.REFRESH_TOKEN.getValue().equalsIgnoreCase(grantTypeKey)
        );
    }

    public static boolean allowsRefresh(RegisteredClient client) {
        return client.getAuthorizationGrantTypes().stream().anyMatch(g -> isRefreshGrant(g.getValue()));
    }

    public static GrantTypeExtension get(String grantTypeKey) {
        if (isRefreshGrant(grantTypeKey)) {
            return CUSTOM_REFRESH_TOKEN;
        }
        for (GrantTypeExtension value : GrantTypeExtension.values()) {
            if (grantTypeKey.equalsIgnoreCase(value.key)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Wrong GrantTypeExtension value. [" + grantTypeKey + "] not in->" + Arrays.asList(GrantTypeExtension.values()));
    }

    public static AuthorizationGrantType toAuthorizationGrantType(String grantTypeKey) {
        return new AuthorizationGrantType(get(grantTypeKey).getKey());
    }

    public String getKey() {
        return key;
    }
}
