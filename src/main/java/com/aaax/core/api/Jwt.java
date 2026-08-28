package com.aaax.core.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Jwt {
    @JsonProperty(OAuth2ParameterNames.ACCESS_TOKEN)
    private String accessToken;
    @JsonProperty(OAuth2ParameterNames.REFRESH_TOKEN)
    private String refreshToken;
    @JsonProperty(OAuth2ParameterNames.EXPIRES_IN)
    private long expiresIn;
    @JsonProperty(OAuth2ParameterNames.TOKEN_TYPE)
    private String tokenType;
    private String bearerToken;
}
