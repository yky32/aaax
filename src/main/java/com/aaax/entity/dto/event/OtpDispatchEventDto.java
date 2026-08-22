package com.aaax.entity.dto.event;

import java.time.Instant;

/**
 * OTP dispatch payload for external notification (Kafka / SMS webhook).
 */
public record OtpDispatchEventDto(
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
