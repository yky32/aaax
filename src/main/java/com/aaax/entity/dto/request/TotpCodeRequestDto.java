package com.aaax.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TotpCodeRequestDto(
        @NotBlank @Size(min = 6, max = 6) String code,
        Boolean rememberDevice,
        @Size(max = 128) String deviceLabel
) {
    public TotpCodeRequestDto(String code) {
        this(code, null, null);
    }
}
