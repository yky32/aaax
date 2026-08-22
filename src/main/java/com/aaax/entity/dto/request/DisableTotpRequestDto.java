package com.aaax.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisableTotpRequestDto(
        @NotBlank String password,
        @NotBlank @Size(min = 6, max = 6) String code
) {
}
