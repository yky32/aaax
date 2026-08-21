package com.aaax.entity.dto;

import java.time.Instant;

/**
 * Canonical OTP dispatch payload for external notification services.
 * Published to Kafka (channel=kafka) or POSTed to webhook (channel=sms).
 */
public record OtpDispatchEvent(
        String eventType,
        String username,
        String destination,
        String channel,
        String code,
        String purpose,
        Instant expiresAt,
        String issuer
) {
    public static final String TYPE = "aaax.otp.dispatch";
}
