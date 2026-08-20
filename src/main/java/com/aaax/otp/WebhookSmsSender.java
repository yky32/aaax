package com.aaax.otp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import tools.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Mode 2 — configurable own SMS provider via HTTP webhook.
 * POST JSON to caller's notification-service; AAAX never talks to Twilio directly.
 */
@Component
@ConditionalOnProperty(name = "aaax.otp.channel", havingValue = "sms")
public class WebhookSmsSender implements SmsSender, OtpSender {

    private static final Logger log = LoggerFactory.getLogger(WebhookSmsSender.class);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper;
    private final String webhookUrl;
    private final String authHeader;
    private final String issuer;

    public WebhookSmsSender(
            ObjectMapper objectMapper,
            @Value("${aaax.otp.sms.webhook-url:}") String webhookUrl,
            @Value("${aaax.otp.sms.webhook-auth:}") String authHeader,
            @Value("${aaax.issuer:http://localhost:8081}") String issuer) {
        this.objectMapper = objectMapper;
        this.webhookUrl = webhookUrl;
        this.authHeader = authHeader;
        this.issuer = issuer;
    }

    @Override
    public void send(String destination, String code) {
        OtpDispatchEvent event = new OtpDispatchEvent(
                OtpDispatchEvent.TYPE,
                destination,
                destination,
                "sms",
                code,
                "otp",
                java.time.Instant.now().plusSeconds(300),
                issuer);
        sendSms(destination, "Your AAAX code is " + code, event);
    }

    @Override
    public void sendSms(String phone, String messageBody, OtpDispatchEvent event) {
        if (!StringUtils.hasText(webhookUrl)) {
            log.warn("SMS webhook URL empty — console fallback. phone={} code={}", phone, event.code());
            log.info("AAAX OTP for {} => {}", phone, event.code());
            return;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("eventType", event.eventType());
            body.put("channel", "sms");
            body.put("to", phone);
            body.put("message", messageBody);
            body.put("code", event.code());
            body.put("username", event.username());
            body.put("purpose", event.purpose());
            body.put("expiresAt", event.expiresAt().toString());
            body.put("issuer", event.issuer());

            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            if (StringUtils.hasText(authHeader)) {
                b.header("authorization", authHeader);
            }
            HttpResponse<String> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                log.error("SMS webhook failed status={} body={}", resp.statusCode(), resp.body());
                throw new IllegalStateException("SMS webhook returned " + resp.statusCode());
            }
            log.info("AAAX OTP SMS webhook ok to={}", phone);
        } catch (Exception ex) {
            throw new IllegalStateException("SMS webhook dispatch failed", ex);
        }
    }
}
