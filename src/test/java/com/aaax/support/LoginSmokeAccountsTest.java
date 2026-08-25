package com.aaax.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginSmokeAccountsTest {

    @Test
    @DisplayName("smoke accounts are fixed dual identities with canonical emails")
    void accounts_areStableAndCanonical() {
        assertEquals("uaa.smoke.primary@tgt.gg", LoginSmokeAccounts.PRIMARY.canonicalEmail());
        assertEquals("uaa.smoke.secondary@tgt.gg", LoginSmokeAccounts.SECONDARY.canonicalEmail());
        assertNotEquals(LoginSmokeAccounts.PRIMARY.email(), LoginSmokeAccounts.SECONDARY.email());
        assertNotEquals(LoginSmokeAccounts.PRIMARY.password(), LoginSmokeAccounts.SECONDARY.password());
        assertEquals(
                LoginSmokeAccounts.PRIMARY.canonicalEmail(),
                LoginSmokeAccounts.PRIMARY_EMAIL_MIXED_CASE.trim().toLowerCase()
        );
        assertEquals("client", LoginSmokeAccounts.OAUTH_CLIENT_ID);
        assertEquals("custom-password-grant", LoginSmokeAccounts.GRANT_TYPE_CUSTOM_PASSWORD);
    }
}
