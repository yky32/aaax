package com.aaax.web;

import java.util.LinkedHashMap;
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
        Map<String, Object> endpoints = new LinkedHashMap<>();
        endpoints.put("register", "POST /v1/accounts/register");
        endpoints.put("login", "POST /v1/auth/login");
        endpoints.put("me", "GET /v1/accounts/me");
        endpoints.put("otpRequest", "POST /v1/otp/request");
        endpoints.put("otpLogin", "POST /v1/auth/otp/login");
        endpoints.put("admin", "/admin/");
        endpoints.put("adminEvents", "GET /v1/admin/events");
        endpoints.put("adminSettings", "GET /v1/admin/settings");
        endpoints.put("apiHello", "GET /v1/api/hello (Bearer + scope api.read)");
        endpoints.put("token", "POST /oauth2/token");
        endpoints.put("health", "/actuator/health");
        endpoints.put("oidc", issuer + "/.well-known/openid-configuration");

        Map<String, Object> docs = new LinkedHashMap<>();
        docs.put("booklet", "docs/AAAX_BOOKLET.md");
        docs.put("events", "docs/IDENTITY_EVENTS.md");
        docs.put("smsSaml", "docs/SMS_SAML.md");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("product", "AAAX");
        body.put("expand", "Accounts · Authentication · Authorization · eXperiences");
        body.put("tagline", "Identity you run. Signals you own.");
        body.put("wedge", "AAAX authenticates. Your mesh notifies.");
        body.put("version", "0.4.0-SNAPSHOT");
        body.put("issuer", issuer);
        body.put("endpoints", endpoints);
        body.put("docs", docs);
        return body;
    }
}
