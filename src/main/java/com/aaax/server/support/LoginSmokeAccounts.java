package com.aaax.server.support;

/**
 * Fixed AAAX login smoke accounts for quality gates and {@code aaax.local-seed}.
 * <p>
 * Local clone only — never use these credentials in production.
 * Password grant: {@code POST /oauth2/token} + {@code grant_type=custom-password-grant}
 * + Basic {@code client:secret}.
 */
public final class LoginSmokeAccounts {

    private LoginSmokeAccounts() {
    }

    /**
     * Primary smoke user — happy-path password grant.
     */
    public static final Account PRIMARY = new Account(
            "smoke.primary@aaax.local",
            "SmokePrimary!1"
    );

    /**
     * Secondary smoke user — second identity + isolation checks.
     */
    public static final Account SECONDARY = new Account(
            "smoke.secondary@aaax.local",
            "SmokeSecondary!2"
    );

    /**
     * Same mailbox as {@link #PRIMARY} with different casing — asserts case-insensitive login (#45).
     */
    public static final String PRIMARY_EMAIL_MIXED_CASE = "Smoke.Primary@Aaax.Local";

    public static final String OAUTH_CLIENT_ID = "client";
    public static final String OAUTH_CLIENT_SECRET = "secret";
    /** Public local client: authorization_code + PKCE only (no client_secret). */
    public static final String OAUTH_PKCE_CLIENT_ID = "aaax-pkce";
    public static final String OAUTH_PKCE_REDIRECT_URI = "http://127.0.0.1:8081/authorized";
    /** IPv6 loopback; SAS still allows any port on this host (RFC 8252 §7.3). */
    public static final String OAUTH_PKCE_REDIRECT_URI_V6 = "http://[::1]:8081/authorized";
    public static final String GRANT_TYPE_CUSTOM_PASSWORD = "custom-password-grant";

    public record Account(String email, String password) {
        public String canonicalEmail() {
            return email == null ? null : email.trim().toLowerCase();
        }
    }
}
