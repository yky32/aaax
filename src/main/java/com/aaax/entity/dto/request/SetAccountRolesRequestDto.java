package com.aaax.entity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SetAccountRolesRequestDto(@NotBlank String roles) {
}
