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
                "version", "0.3.0",
                "issuer", issuer,
                "endpoints", Map.of(
                        "register", "POST /v1/accounts/register",
                        "me", "GET /v1/accounts/me",
                        "otpRequest", "POST /v1/otp/request",
                        "otpVerify", "POST /v1/otp/verify",
                        "otpLogin", "POST /v1/auth/otp/login",
                        "adminClients", " /v1/admin/clients",
                        "apiHello", "GET /v1/api/hello (Bearer + scope api.read)",
                        "token", "POST /oauth2/token",
                        "health", "/actuator/health",
                        "oidc", issuer + "/.well-known/openid-configuration"),
                "docs", Map.of("booklet", "docs/AAAX_BOOKLET.md"));
    }
}
