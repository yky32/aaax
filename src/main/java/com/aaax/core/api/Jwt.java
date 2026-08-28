package com.aaax.core.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("access_token")
    @JsonAlias("accessToken")
    private String accessToken;
    @JsonProperty("refresh_token")
    @JsonAlias("refreshToken")
    private String refreshToken;
    @JsonProperty("expires_in")
    @JsonAlias("expiresIn")
    private long expiresIn;
    @JsonProperty("token_type")
    @JsonAlias("tokenType")
    private String tokenType;
    private String bearerToken; // for JWT header.
}
