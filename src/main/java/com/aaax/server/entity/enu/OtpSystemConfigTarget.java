package com.aaax.server.entity.enu;

/**
 * {@code system_configuration.target} keys for OTP (values are seconds).
 */
public interface OtpSystemConfigTarget {

    /** How long the OTP code stays valid in Redis (verify window). */
    String OTP_TTL = "OTP_TTL";

    /** Min seconds between OTP generate / resend attempts. */
    String OTP_RESEND_TTL = "OTP_RESEND_TTL";

    /** Forgot-password OTP code live window. */
    String OTP_RESET_PASSWORD_TTL = "OTP_RESET_PASSWORD_TTL";
}
