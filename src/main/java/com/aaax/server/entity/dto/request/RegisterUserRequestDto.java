package com.aaax.server.entity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterUserRequestDto {
    private String username;
    private String code; // ONLY WHEN fill OTP case
    private String credentials;
    private List<String> extraFeatures = new ArrayList<>(); // INTERNAL USE.
    private Map metadata;
    private String sourceSystem;
}
