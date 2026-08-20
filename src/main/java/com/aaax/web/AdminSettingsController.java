package com.aaax.web;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aaax.account.AccountService;
import com.aaax.audit.AuditEvent;
import com.aaax.audit.AuditService;
import com.aaax.client.ClientAdminService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin")
public class AdminSettingsController {

    private final AccountService accountService;
    private final ClientAdminService clientAdminService;
    private final AuditService auditService;
    private final Environment environment;
    private final String issuer;
    private final String otpChannel;
    private final boolean seedClient;
    private final boolean seedAccount;

    public AdminSettingsController(
            AccountService accountService,
            ClientAdminService clientAdminService,
            AuditService auditService,
            Environment environment,
            @Value("${aaax.issuer:http://localhost:8081}") String issuer,
            @Value("${aaax.otp.channel:console}") String otpChannel,
            @Value("${aaax.demo.seed-client:true}") boolean seedClient,
            @Value("${aaax.demo.seed-account:true}") boolean seedAccount) {
        this.accountService = accountService;
        this.clientAdminService = clientAdminService;
        this.auditService = auditService;
        this.environment = environment;
        this.issuer = issuer;
        this.otpChannel = otpChannel;
        this.seedClient = seedClient;
        this.seedAccount = seedAccount;
    }

    @GetMapping("/settings")
    public Map<String, Object> settings() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("product", "AAAX");
        m.put("version", "0.4.0-SNAPSHOT");
        m.put("issuer", issuer);
        m.put("otpChannel", otpChannel);
        m.put("demoSeedClient", seedClient);
        m.put("demoSeedAccount", seedAccount);
        m.put("profiles", List.of(environment.getActiveProfiles()));
        m.put("googleLoginEnabled", hasText(environment.getProperty("spring.security.oauth2.client.registration.google.client-id")));
        m.put("counts", Map.of(
                "users", accountService.countUsers(),
                "admins", accountService.countAdmins(),
                "clients", clientAdminService.list().size()));
        m.put("features", Map.of(
                "totpMfa", true,
                "otpPasswordless", true,
                "adminPortal", true,
                "oauthClientsAdmin", true,
                "bootstrapAdmin", true,
                "googleOidc", hasText(environment.getProperty("spring.security.oauth2.client.registration.google.client-id")),
                "orgs", false,
                "saml", false,
                "smsOtp", false,
                "passkeys", false));
        m.put("decisionBlockers", List.of(
                Map.of("id", "sms_provider", "question", "Which SMS provider? (Twilio / others / none)"),
                Map.of("id", "orgs_model", "question", "Multi-tenant orgs: single-realm only vs orgs/teams?"),
                Map.of("id", "saml", "question", "Ship SAML IdP/SP in v1 or defer?"),
                Map.of("id", "passkeys", "question", "Passkeys priority vs social pack depth?")));
        return m;
    }

    @GetMapping("/audit")
    public List<Map<String, Object>> audit(@RequestParam(defaultValue = "50") int limit, Principal principal) {
        return auditService.recent(limit).stream().map(this::mapEvent).toList();
    }

    private Map<String, Object> mapEvent(AuditEvent e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("action", e.getAction());
        m.put("actor", e.getActor());
        m.put("detail", e.getDetail());
        m.put("createdAt", e.getCreatedAt());
        return m;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
