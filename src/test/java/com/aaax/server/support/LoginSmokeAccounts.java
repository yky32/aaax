package com.aaax.server.support;

/**
 * Fixed UAA login smoke accounts for quality gates (unit / Testcontainers / env smoke).
 * <p>
 * Never use real customer accounts. Password grant path:
 * {@code POST /oauth2/token} + {@code grant_type=custom-password-grant}
 * + Basic {@code client:secret}.
 */
public final class LoginSmokeAccounts {

    private LoginSmokeAccounts() {
    }

    /**
     * Primary smoke user — happy-path password grant.
     */
    public static final Account PRIMARY = new Account(
            "uaa.smoke.primary@aaax.local",
            "SmokePrimary!1"
    );

    /**
     * Secondary smoke user — second identity + isolation checks.
     */
    public static final Account SECONDARY = new Account(
            "uaa.smoke.secondary@aaax.local",
            "SmokeSecondary!2"
    );

    /**
     * Same mailbox as {@link #PRIMARY} with different casing — asserts case-insensitive login (#45).
     */
    public static final String PRIMARY_EMAIL_MIXED_CASE = "Uaa.Smoke.Primary@Aaax.Local";

    public static final String OAUTH_CLIENT_ID = "client";
    public static final String OAUTH_CLIENT_SECRET = "secret";
    public static final String GRANT_TYPE_CUSTOM_PASSWORD = "custom-password-grant";

    public record Account(String email, String password) {
        public String canonicalEmail() {
            return email == null ? null : email.trim().toLowerCase();
        }
    }
}
