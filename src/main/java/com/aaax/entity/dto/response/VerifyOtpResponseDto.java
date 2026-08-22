package com.aaax.entity.dto.response;

public record VerifyOtpResponseDto(
        boolean valid,
        String username
) {
}
