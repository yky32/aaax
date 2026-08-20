package com.aaax.events;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CloudEvents-shaped identity signal. AAAX's competitive wedge:
 * platform teams own notification-service — IdP emits events, not SMS vendors.
 */
public record IdentityEvent(
        String specversion,
        String id,
        String source,
        String type,
        String time,
        String subject,
        Map<String, Object> data
) {
    public static final String SPEC = "1.0";

    public static IdentityEvent of(String issuer, String type, String subject, Map<String, Object> data) {
        Map<String, Object> payload = data == null ? Map.of() : new LinkedHashMap<>(data);
        return new IdentityEvent(
                SPEC,
                UUID.randomUUID().toString(),
                issuer,
                type,
                Instant.now().toString(),
                subject,
                payload);
    }

    public static final class Types {
        public static final String ACCOUNT_REGISTERED = "com.aaax.account.registered";
        public static final String AUTH_LOGIN = "com.aaax.auth.login";
        public static final String AUTH_LOGIN_MFA = "com.aaax.auth.login.mfa";
        public static final String AUTH_LOGIN_SOCIAL = "com.aaax.auth.login.social";
        public static final String AUTH_LOGOUT = "com.aaax.auth.logout";
        public static final String ACCOUNT_FEDERATED = "com.aaax.account.federated";
        public static final String MFA_TOTP_ENABLED = "com.aaax.mfa.totp.enabled";
        public static final String MFA_TOTP_DISABLED = "com.aaax.mfa.totp.disabled";
        public static final String PASSWORD_CHANGED = "com.aaax.account.password.changed";
        public static final String PASSWORD_RESET = "com.aaax.account.password.reset";
        public static final String OTP_DISPATCH = "com.aaax.otp.dispatch";
        public static final String CLIENT_CREATED = "com.aaax.client.created";
        public static final String CLIENT_DELETED = "com.aaax.client.deleted";
        public static final String BOOTSTRAP_ADMIN = "com.aaax.admin.bootstrap";
        public static final String USER_STATUS = "com.aaax.admin.user.status";
        public static final String USER_ROLES = "com.aaax.admin.user.roles";

        private Types() {
        }
    }
}
