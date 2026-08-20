package com.aaax.web;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetaController {

    private final String issuer;

    public MetaController(@Value("${aaax.issuer:http://localhost:8081}") String issuer) {
        this.issuer = issuer;
    }

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "product", "AAAX",
                "expand", "Accounts · Authentication · Authorization · eXperiences",
                "version", "0.1.0-SNAPSHOT",
                "issuer", issuer,
                "docs", Map.of(
                        "vision", "VISION.md",
                        "roadmap", "ROADMAP.md",
                        "oidc", issuer + "/.well-known/openid-configuration"));
    }
}
