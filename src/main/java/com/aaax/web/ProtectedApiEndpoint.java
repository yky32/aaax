package com.aaax.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Protected resource sample — requires JWT with scope api.read (or ROLE_ADMIN for /admin).
 */
@RestController
@RequestMapping("/v1/api")
public class ProtectedApiEndpoint {

    @GetMapping("/hello")
    public Map<String, Object> hello(Authentication authentication) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "hello from AAAX protected API");
        body.put("principal", authentication.getName());
        body.put("authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList());
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            body.put("client_id", jwt.getClaimAsString("client_id"));
            body.put("scope", jwt.getClaimAsString("scope"));
            body.put("sub", jwt.getSubject());
        }
        return body;
    }

    @GetMapping("/admin/ping")
    public Map<String, Object> adminPing(Authentication authentication) {
        return Map.of(
                "message", "admin ok",
                "principal", authentication.getName());
    }
}
