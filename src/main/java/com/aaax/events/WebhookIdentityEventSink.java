package com.aaax.events;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import tools.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * HTTP fan-out to caller notification / automation endpoint.
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

    public WebhookIdentityEventSink(
            ObjectMapper objectMapper,
            @Value("${aaax.events.webhook-url}") String url,
            @Value("${aaax.events.webhook-auth:}") String auth) {
        this.objectMapper = objectMapper;
        this.url = url;
        this.auth = auth;
    }

    @Override
    public void publish(IdentityEvent event) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("content-type", "application/json")
                    .header("ce-id", event.id())
                    .header("ce-type", event.type())
                    .header("ce-source", event.source())
                    .header("ce-specversion", event.specversion())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(event)));
            if (StringUtils.hasText(auth)) {
                b.header("authorization", auth);
            }
            HttpResponse<String> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                log.warn("Identity event webhook status={} type={}", resp.statusCode(), event.type());
            }
        } catch (Exception ex) {
            log.warn("Identity event webhook failed type={}: {}", event.type(), ex.toString());
        }
    }
}
