package com.aaax.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequestDto(
        @NotBlank @Size(max = 320) String usernameOrEmail
) {
}
