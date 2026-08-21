package com.aaax.events;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Frozen public event catalog (P1).
 * Breaking type renames require a new {@link #VERSION} and booklet note.
 */
public final class IdentityEventCatalog {

    /** Catalog contract version — bump only on breaking type/schema changes. */
    public static final String VERSION = "1.0";

    public static final String DATASCHEMA_PREFIX = "aaax:events/catalog/" + VERSION + "#";

    private IdentityEventCatalog() {
    }

    public static String dataschema(String type) {
        return DATASCHEMA_PREFIX + type;
    }

    /** All types AAAX may emit today (stable). */
    public static List<String> types() {
        return List.of(
                IdentityEvent.Types.ACCOUNT_REGISTERED,
                IdentityEvent.Types.AUTH_LOGIN,
                IdentityEvent.Types.AUTH_LOGIN_MFA,
                IdentityEvent.Types.AUTH_LOGIN_SOCIAL,
                IdentityEvent.Types.AUTH_LOGOUT,
                IdentityEvent.Types.AUTH_QR_CREATED,
                IdentityEvent.Types.AUTH_QR_APPROVED,
                IdentityEvent.Types.ACCOUNT_FEDERATED,
                IdentityEvent.Types.MFA_TOTP_ENABLED,
                IdentityEvent.Types.MFA_TOTP_DISABLED,
                IdentityEvent.Types.PASSWORD_CHANGED,
                IdentityEvent.Types.PASSWORD_RESET,
                IdentityEvent.Types.OTP_DISPATCH,
                IdentityEvent.Types.DEVICE_TRUSTED,
                IdentityEvent.Types.CLIENT_CREATED,
                IdentityEvent.Types.CLIENT_DELETED,
                IdentityEvent.Types.BOOTSTRAP_ADMIN,
                IdentityEvent.Types.USER_STATUS,
                IdentityEvent.Types.USER_ROLES);
    }

    public static Map<String, Object> describe() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("catalogVersion", VERSION);
        m.put("specversion", IdentityEvent.SPEC);
        m.put("compatibility", "additive types OK within major catalog version; renames = bump VERSION");
        m.put("types", types());
        m.put("otpDispatchData", List.of(
                "username", "destination", "channel", "code", "purpose", "expiresAt", "eventId"));
        m.put("webhookHeaders", List.of(
                "content-type",
                "ce-id",
                "ce-type",
                "ce-source",
                "ce-specversion",
                "x-aaax-event-id",
                "x-aaax-delivery-id",
                "x-aaax-signature (if secret set)",
                "authorization (optional)"));
        return m;
    }
}
