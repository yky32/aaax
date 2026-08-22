package com.aaax.entity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterAccountRequestDto(
        @NotBlank
        @Size(min = 3, max = 64)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "username may contain letters, digits, . _ - only")
        String username,

        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(min = 8, max = 128)
        String password
) {
}
