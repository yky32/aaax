package com.aaax.core.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Jwt {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String tokenType;
    private String bearerToken; // for JWT header.
}
