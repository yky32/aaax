package com.aaax.server.config.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class JwtPayload {
    private String sub; // userId;
    private List<String> aud;
    private JwtMetadata metadata;
    private Integer nbf;
    private List<String> scope;
    private String iss;
    private Integer exp;
    private Integer iat;
}
