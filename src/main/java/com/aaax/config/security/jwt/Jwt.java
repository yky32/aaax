package com.aaax.config.security.jwt;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Jwt {
    private String principalName;
    private String accessToken;
    private Instant accessTokenIssuedAt;
    private Instant accessTokenExpiresAt;
    private String refreshToken;
    private Instant refreshTokenIssuedAt;
    private Instant refreshTokenExpiresAt;
    private String idToken;
    private JwtPayload payload;
    private RegisteredClientMetadata registeredClientMetadata;
    private String authorizationGrantType;
    private Set<String> scopes;
    private String tokenType;
    private long expiresIn; // this field is used to show [counting-down-effect]
}
