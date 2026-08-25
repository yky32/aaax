package com.aaax.config.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class ClientCredentialsJwt {
    private String accessToken;
}