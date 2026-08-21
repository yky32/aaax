package com.aaax.events;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import tools.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * HTTP fan-out with optional HMAC signature, delivery id, and retries.
 *
 * <p>Headers:
 * <ul>
 *   <li>{@code X-AAAX-Event-Id} / {@code X-AAAX-Delivery-Id} = CloudEvents id (idempotency key)</li>
 *   <li>{@code X-AAAX-Signature} = {@code sha256=<hex>} of raw body when secret configured</li>
 *   <li>standard {@code ce-*} CloudEvents headers</li>
 * </ul>
 */
@Component
@Order(20)
@ConditionalOnProperty(name = "aaax.events.webhook-url")
public class WebhookIdentityEventSink implements IdentityEventSink {

    private static final Logger log = LoggerFactory.getLogger(WebhookIdentityEventSink.class);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper;
    private final String url;
    private final String auth;
    private final String secret;
    private final int maxAttempts;
    private final long backoffMs;

    public WebhookIdentityEventSink(
            ObjectMapper objectMapper,
            @Value("${aaax.events.webhook-url}") String url,
            @Value("${aaax.events.webhook-auth:}") String auth,
            @Value("${aaax.events.webhook-secret:}") String secret,
            @Value("${aaax.events.webhook-max-attempts:3}") int maxAttempts,
            @Value("${aaax.events.webhook-backoff-ms:200}") long backoffMs) {
        this.objectMapper = objectMapper;
        this.url = url;
        this.auth = auth;
        this.secret = secret;
        this.maxAttempts = Math.max(1, Math.min(maxAttempts, 8));
        this.backoffMs = Math.max(50, backoffMs);
    }

    @Override
    public void publish(IdentityEvent event) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        final String body;
        try {
            body = objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            log.warn("Identity event webhook serialize failed type={}: {}", event.type(), ex.toString());
            return;
        }

        Exception last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpRequest.Builder b = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(8))
                        .header("content-type", "application/json")
                        .header("ce-id", event.id())
                        .header("ce-type", event.type())
                        .header("ce-source", event.source())
                        .header("ce-specversion", event.specversion())
                        .header("x-aaax-event-id", event.id())
                        .header("x-aaax-delivery-id", event.id())
                        .header("x-aaax-attempt", String.valueOf(attempt))
                        .POST(HttpRequest.BodyPublishers.ofString(body));
                if (StringUtils.hasText(auth)) {
                    b.header("authorization", auth);
                }
                if (StringUtils.hasText(secret)) {
                    b.header("x-aaax-signature", "sha256=" + hmacSha256Hex(secret, body));
                }
                HttpResponse<String> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
                int code = resp.statusCode();
                if (code >= 200 && code < 300) {
                    return;
                }
                // retry 408/429/5xx
                if (code == 408 || code == 429 || code >= 500) {
                    log.warn(
                            "Identity event webhook retryable status={} type={} attempt={}/{}",
                            code,
                            event.type(),
                            attempt,
                            maxAttempts);
                    sleepBackoff(attempt);
                    continue;
                }
                log.warn("Identity event webhook status={} type={} (no retry)", code, event.type());
                return;
            } catch (Exception ex) {
                last = ex;
                log.warn(
                        "Identity event webhook failed type={} attempt={}/{}: {}",
                        event.type(),
                        attempt,
                        maxAttempts,
                        ex.toString());
                sleepBackoff(attempt);
            }
        }
        if (last != null) {
            log.warn("Identity event webhook exhausted retries type={} id={}", event.type(), event.id());
        }
    }

    private void sleepBackoff(int attempt) {
        if (attempt >= maxAttempts) {
            return;
        }
        try {
            Thread.sleep(backoffMs * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public static String hmacSha256Hex(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failed", e);
        }
    }
}
