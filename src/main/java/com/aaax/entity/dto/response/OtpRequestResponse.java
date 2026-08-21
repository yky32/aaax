package com.aaax.entity.dto.response;

import java.time.Instant;

public record OtpRequestResponse(
        String username,
        String destination,
        int ttlSeconds,
        Instant expiresAt
) {
}
