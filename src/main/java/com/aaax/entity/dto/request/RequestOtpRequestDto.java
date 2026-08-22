package com.aaax.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestOtpRequestDto(
        @NotBlank @Size(max = 64) String username
) {
}
