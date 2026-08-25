package com.aaax.config.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import static org.junit.jupiter.api.Assertions.*;

class GrantTypeExtensionTest {

    @ParameterizedTest
    @CsvSource({
            "custom-password-grant, CUSTOM_PASSWORD_GRANT",
            "custom-password-grant:e, CUSTOM_PASSWORD_GRANT_ENCRYPTED",
            "refresh-token, CUSTOM_REFRESH_TOKEN",
            "ext-password-grant, EXT_PASSWORD_GRANT",
            "third-party-grant, THIRD_PARTY_OAUTH_GRANT"
    })
    @DisplayName("get should resolve grant type keys")
    void get_shouldResolve(String key, GrantTypeExtension expected) {
        assertEquals(expected, GrantTypeExtension.get(key));
        assertEquals(key, expected.getKey());
    }

    @Test
    @DisplayName("get should throw for unknown key")
    void get_shouldThrowForUnknown() {
        assertThrows(IllegalArgumentException.class, () -> GrantTypeExtension.get("unknown"));
    }

    @Test
    @DisplayName("toAuthorizationGrantType should wrap key")
    void toAuthorizationGrantType_shouldWrap() {
        AuthorizationGrantType type = GrantTypeExtension.toAuthorizationGrantType("custom-password-grant");
        assertEquals("custom-password-grant", type.getValue());
    }
}
