package com.aaax.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDto(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(min = 4, max = 10) String code,
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {
}
