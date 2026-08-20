package com.aaax.examples.rs;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiController {

    @GetMapping("/api/hello")
    public Map<String, Object> hello(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "hello from external resource server");
        body.put("sub", jwt.getSubject());
        body.put("scope", jwt.getClaimAsString("scope"));
        body.put("iss", jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
        body.put("client_id", jwt.getClaimAsString("client_id"));
        return body;
    }
}
