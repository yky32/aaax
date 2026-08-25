package com.aaax.server.config.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class JwtMetadata {
    private String identifier; // = username
    private String sessionId; // = sessionId @ request
    private Map<String, Object> extReferenceMap; // MMID, Google ID;
}
