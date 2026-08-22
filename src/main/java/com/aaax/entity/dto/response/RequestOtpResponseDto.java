package com.aaax.entity.dto.response;

import java.time.Instant;

public record RequestOtpResponseDto(
        String username,
        String destination,
        int ttlSeconds,
        Instant expiresAt
) {
}
