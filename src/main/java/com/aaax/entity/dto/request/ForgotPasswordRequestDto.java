package com.aaax.entity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ForgotPasswordRequestDto {
    private String username;
    private String code;
    private String credentials;
    private Map metadata;
    private String sourceSystem;
}
