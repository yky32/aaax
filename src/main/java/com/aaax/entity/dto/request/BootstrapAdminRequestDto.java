package com.aaax.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BootstrapAdminRequestDto(
        @NotBlank @Size(max = 64) String username,
        @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        String bootstrapToken
) {
}
