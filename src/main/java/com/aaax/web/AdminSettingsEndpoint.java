package com.aaax.web;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.aaax.account.application.AccountQueries;
import com.aaax.audit.AuditEvent;
import com.aaax.audit.AuditService;
import com.aaax.client.ClientAdminService;
import com.aaax.events.BufferIdentityEventSink;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventCatalog;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin")
public class AdminSettingsEndpoint {

    private final AccountQueries accountService;
    private final ClientAdminService clientAdminService;
    private final AuditService auditService;
    private final BufferIdentityEventSink eventBuffer;
    private final Environment environment;
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplate;
    private final String issuer;
    private final String otpChannel;
    private final boolean seedClient;
    private final boolean seedAccount;
    private final boolean samlEnabled;
    private final String smsWebhook;
    private final String otpKafkaTopic;
    private final boolean eventsKafkaEnabled;
    private final String eventsKafkaTopic;
    private final String eventsWebhook;

    public AdminSettingsEndpoint(
            AccountQueries accountService,
            ClientAdminService clientAdminService,
            AuditService auditService,
            BufferIdentityEventSink eventBuffer,
            Environment environment,
            ObjectProvider<KafkaTemplate<String, String>> kafkaTemplate,
            @Value("${aaax.issuer:http://localhost:8081}") String issuer,
            @Value("${aaax.otp.channel:console}") String otpChannel,
            @Value("${aaax.demo.seed-client:true}") boolean seedClient,
            @Value("${aaax.demo.seed-account:true}") boolean seedAccount,
            @Value("${aaax.saml.enabled:false}") boolean samlEnabled,
            @Value("${aaax.otp.sms.webhook-url:}") String smsWebhook,
            @Value("${aaax.otp.kafka.topic:aaax.otp.dispatch}") String otpKafkaTopic,
            @Value("${aaax.events.kafka.enabled:false}") boolean eventsKafkaEnabled,
            @Value("${aaax.events.kafka.topic:aaax.identity.events}") String eventsKafkaTopic,
            @Value("${aaax.events.webhook-url:}") String eventsWebhook) {
        this.accountService = accountService;
        this.clientAdminService = clientAdminService;
        this.auditService = auditService;
        this.eventBuffer = eventBuffer;
        this.environment = environment;
        this.kafkaTemplate = kafkaTemplate;
        this.issuer = issuer;
        this.otpChannel = otpChannel;
        this.seedClient = seedClient;
        this.seedAccount = seedAccount;
        this.samlEnabled = samlEnabled;
        this.smsWebhook = smsWebhook;
        this.otpKafkaTopic = otpKafkaTopic;
        this.eventsKafkaEnabled = eventsKafkaEnabled;
        this.eventsKafkaTopic = eventsKafkaTopic;
        this.eventsWebhook = eventsWebhook;
    }

    @GetMapping("/settings")
    public Map<String, Object> settings() {
        boolean kafkaLive = kafkaTemplate.getIfAvailable() != null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("product", "AAAX");
        m.put("version", "0.7.0-SNAPSHOT");
        m.put("issuer", issuer);
        m.put("otpChannel", otpChannel);
        m.put("orgsModel", "single");
        m.put("demoSeedClient", seedClient);
        m.put("demoSeedAccount", seedAccount);
        m.put("profiles", List.of(environment.getActiveProfiles()));
        m.put(
                "googleLoginEnabled",
                hasText(environment.getProperty("spring.security.oauth2.client.registration.google.client-id")));
        m.put(
                "githubLoginEnabled",
                hasText(environment.getProperty("spring.security.oauth2.client.registration.github.client-id")));
        m.put(
                "socialLoginPath",
                Map.of(
                        "google",
                        "/oauth2/authorization/google",
                        "github",
                        "/oauth2/authorization/github",
                        "providersApi",
                        "/v1/auth/social/providers"));
        m.put("samlEnabled", samlEnabled);
        m.put("samlLoginPath", samlEnabled ? "/saml2/authenticate/idp" : null);
        m.put(
                "identityEventBus",
                Map.of(
                        "enabled",
                        true,
                        "catalogVersion",
                        IdentityEventCatalog.VERSION,
                        "catalogApi",
                        "/v1/admin/events/catalog",
                        "bufferSize",
                        eventBuffer.size(),
                        "kafkaEnabled",
                        kafkaLive || eventsKafkaEnabled || "kafka".equalsIgnoreCase(otpChannel),
                        "kafkaLive",
                        kafkaLive,
                        "kafkaTopic",
                        eventsKafkaTopic,
                        "webhookConfigured",
                        hasText(eventsWebhook),
                        "webhookSigned",
                        hasText(environment.getProperty("aaax.events.webhook-secret")),
                        "wedge",
                        "AAAX authenticates. Your mesh notifies."));
        m.put(
                "otpDispatch",
                Map.of(
                        "channel",
                        otpChannel,
                        "modes",
                        List.of(
                                Map.of("id", "console", "desc", "Log codes (dev)"),
                                Map.of("id", "mail", "desc", "SMTP email"),
                                Map.of("id", "kafka", "desc", "Mode1: OTP on Identity Event Bus → Kafka"),
                                Map.of("id", "sms", "desc", "Mode2: HTTP webhook SMS + bus event")),
                        "smsWebhookConfigured",
                        hasText(smsWebhook),
                        "otpKafkaTopic",
                        otpKafkaTopic,
                        "eventType",
                        IdentityEvent.Types.OTP_DISPATCH));
        m.put(
                "counts",
                Map.of(
                        "users",
                        accountService.countUsers(),
                        "admins",
                        accountService.countAdmins(),
                        "clients",
                        clientAdminService.list().size(),
                        "eventsBuffered",
                        eventBuffer.size()));
        m.put("features", featureMap(kafkaLive));
        m.put(
                "decisionBlockers",
                List.of(
                        Map.of("id", "orgs_multi", "question", "Multi-tenant organizations (Clerk Orgs) — still single-realm"),
                        Map.of("id", "saml_idp", "question", "SAML IdP (AAAX as IdP) — SP done; full IdP later?"),
                        Map.of("id", "qr_login", "question", "QR login — shipped (in-memory); multi-node store later?"),
                        Map.of(
                                "id",
                                "device_binding",
                                "question",
                                "Strict device allow-list (block unknown) — trust/skip-MFA shipped"),
                        Map.of("id", "react_sdk", "question", "Official React/Next SDK — hosted pages first")));
        return m;
    }

    @GetMapping("/events")
    public List<Map<String, Object>> events(@RequestParam(defaultValue = "50") int limit) {
        return eventBuffer.recent(limit).stream().map(this::mapIdentity).collect(Collectors.toList());
    }

    @GetMapping("/events/catalog")
    public Map<String, Object> eventCatalog() {
        return IdentityEventCatalog.describe();
    }

    @GetMapping("/audit")
    public List<Map<String, Object>> audit(@RequestParam(defaultValue = "50") int limit, Principal principal) {
        return auditService.recent(limit).stream().map(this::mapAudit).toList();
    }

    private Map<String, Object> featureMap(boolean kafkaLive) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("identityEventBus", true);
        f.put("eventCatalogVersion", IdentityEventCatalog.VERSION);
        f.put("totpMfa", true);
        f.put("otpPasswordless", true);
        f.put("adminPortal", true);
        f.put("oauthClientsAdmin", true);
        f.put("bootstrapAdmin", true);
        f.put(
                "googleOidc",
                hasText(environment.getProperty("spring.security.oauth2.client.registration.google.client-id")));
        f.put(
                "githubOAuth",
                hasText(environment.getProperty("spring.security.oauth2.client.registration.github.client-id")));
        f.put(
                "socialLogin",
                hasText(environment.getProperty("spring.security.oauth2.client.registration.google.client-id"))
                        || hasText(environment.getProperty("spring.security.oauth2.client.registration.github.client-id")));
        f.put("samlSp", samlEnabled);
        f.put("passkeys", environment.getProperty("aaax.passkeys.enabled", "false"));
        f.put(
                "passkeysStatus",
                "true".equalsIgnoreCase(environment.getProperty("aaax.passkeys.enabled", "false"))
                        ? "webauthn4j-verified"
                        : "disabled");
        f.put("magicLink", true);
        f.put("qrLogin", true);
        f.put("trustedDevices", true);
        f.put("otpStore", environment.getProperty("aaax.otp.store", "memory"));
        f.put("hostedExperiences", true);
        f.put("sessions", true);
        f.put("orgsModel", "single");
        f.put("smsOtpWebhook", "sms".equalsIgnoreCase(otpChannel));
        f.put("smsOtpKafka", "kafka".equalsIgnoreCase(otpChannel));
        f.put("eventsKafka", kafkaLive || eventsKafkaEnabled);
        f.put("eventsWebhook", hasText(eventsWebhook));
        f.put("orgs", false);
        return f;
    }

    private Map<String, Object> mapIdentity(IdentityEvent e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("specversion", e.specversion());
        m.put("id", e.id());
        m.put("source", e.source());
        m.put("type", e.type());
        m.put("time", e.time());
        m.put("subject", e.subject());
        m.put("dataschema", e.dataschema());
        m.put("data", e.data());
        return m;
    }

    private Map<String, Object> mapAudit(AuditEvent e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("eventId", e.getEventId());
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
