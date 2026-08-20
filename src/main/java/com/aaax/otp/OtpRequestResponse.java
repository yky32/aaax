package com.aaax.otp;

import java.time.Instant;

public record OtpRequestResponse(
        String username,
        String destination,
        int ttlSeconds,
        Instant expiresAt
) {
}
