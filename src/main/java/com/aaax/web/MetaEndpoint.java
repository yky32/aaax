package com.aaax.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetaEndpoint {

    private final String issuer;
    private final boolean passkeysEnabled;

    public MetaEndpoint(
            @Value("${aaax.issuer:http://localhost:8081}") String issuer,
            @Value("${aaax.passkeys.enabled:false}") boolean passkeysEnabled) {
        this.issuer = issuer;
        this.passkeysEnabled = passkeysEnabled;
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
        if (passkeysEnabled) {
            endpoints.put("passkeys", "/v1/passkeys/*");
        }

        Map<String, Object> docs = new LinkedHashMap<>();
        docs.put("booklet", "docs/booklet.md");
        docs.put("note", "Single SoT — other docs/* are stubs");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("product", "AAAX");
        body.put("expand", "Accounts · Authentication · Authorization · eXperiences");
        body.put("tagline", "Identity you run. Signals you own.");
        body.put("wedge", "AAAX authenticates. Your mesh notifies.");
        body.put("hosted", Map.of(
                "signIn", "/sign-in/",
                "signUp", "/sign-up/",
                "user", "/user/",
                "admin", "/admin/"));
        body.put("version", "0.6.0");
        body.put("issuer", issuer);
        body.put("features", Map.of(
                "passkeys", passkeysEnabled ? "webauthn4j" : "disabled",
                "qrLogin", true,
                "trustedDevices", true,
                "otpStore", "memory|redis",
                "eventBus", true));
        body.put("endpoints", endpoints);
        body.put("docs", docs);
        return body;
    }
}
