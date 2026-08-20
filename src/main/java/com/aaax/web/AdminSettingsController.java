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
    private final boolean samlEnabled;
    private final String smsWebhook;
    private final String kafkaTopic;

    public AdminSettingsController(
            AccountService accountService,
            ClientAdminService clientAdminService,
            AuditService auditService,
            Environment environment,
            @Value("${aaax.issuer:http://localhost:8081}") String issuer,
            @Value("${aaax.otp.channel:console}") String otpChannel,
            @Value("${aaax.demo.seed-client:true}") boolean seedClient,
            @Value("${aaax.demo.seed-account:true}") boolean seedAccount,
            @Value("${aaax.saml.enabled:false}") boolean samlEnabled,
            @Value("${aaax.otp.sms.webhook-url:}") String smsWebhook,
            @Value("${aaax.otp.kafka.topic:aaax.otp.dispatch}") String kafkaTopic) {
        this.accountService = accountService;
        this.clientAdminService = clientAdminService;
        this.auditService = auditService;
        this.environment = environment;
        this.issuer = issuer;
        this.otpChannel = otpChannel;
        this.seedClient = seedClient;
        this.seedAccount = seedAccount;
        this.samlEnabled = samlEnabled;
        this.smsWebhook = smsWebhook;
        this.kafkaTopic = kafkaTopic;
    }

    @GetMapping("/settings")
    public Map<String, Object> settings() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("product", "AAAX");
        m.put("version", "0.4.0-SNAPSHOT");
        m.put("issuer", issuer);
        m.put("otpChannel", otpChannel);
        m.put("orgsModel", "single");
        m.put("demoSeedClient", seedClient);
        m.put("demoSeedAccount", seedAccount);
        m.put("profiles", List.of(environment.getActiveProfiles()));
        m.put("googleLoginEnabled", hasText(environment.getProperty("spring.security.oauth2.client.registration.google.client-id")));
        m.put("samlEnabled", samlEnabled);
        m.put("samlLoginPath", samlEnabled ? "/saml2/authenticate/idp" : null);
        m.put("otpDispatch", Map.of(
                "channel", otpChannel,
                "modes", List.of(
                        Map.of("id", "console", "desc", "Log codes (dev)"),
                        Map.of("id", "mail", "desc", "SMTP email"),
                        Map.of("id", "kafka", "desc", "Mode1: publish OtpDispatchEvent — caller owns SMS"),
                        Map.of("id", "sms", "desc", "Mode2: HTTP webhook to caller's notification-service")),
                "smsWebhookConfigured", hasText(smsWebhook),
                "kafkaTopic", kafkaTopic));
        m.put("counts", Map.of(
                "users", accountService.countUsers(),
                "admins", accountService.countAdmins(),
                "clients", clientAdminService.list().size()));
        m.put("features", featureMap());
        m.put("decisionBlockers", List.of(
                Map.of("id", "passkeys", "question", "Passkeys — deferred (later)"),
                Map.of("id", "saml_idp", "question", "SAML IdP (AAAX as IdP for SAML apps) — SP done; full IdP later?")));
        return m;
    }

    private Map<String, Object> featureMap() {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("totpMfa", true);
        f.put("otpPasswordless", true);
        f.put("adminPortal", true);
        f.put("oauthClientsAdmin", true);
        f.put("bootstrapAdmin", true);
        f.put("googleOidc", hasText(environment.getProperty("spring.security.oauth2.client.registration.google.client-id")));
        f.put("samlSp", samlEnabled);
        f.put("orgs", false);
        f.put("orgsModel", "single");
        f.put("smsOtpWebhook", "sms".equalsIgnoreCase(otpChannel));
        f.put("smsOtpKafka", "kafka".equalsIgnoreCase(otpChannel));
        f.put("passkeys", false);
        return f;
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
