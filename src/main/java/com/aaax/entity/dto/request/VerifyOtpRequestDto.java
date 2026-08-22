package com.aaax.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyOtpRequestDto(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(min = 4, max = 10) String code
) {
}
