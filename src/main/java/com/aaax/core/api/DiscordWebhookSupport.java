package com.aaax.core.api;

import com.aaax.core.api.dto.DiscordWebhookMessage;
import com.aaax.core.utils.RetrofitCallHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Discord webhooks are optional for OSS — blank id/token = no-op.
 */
public final class DiscordWebhookSupport {

    private static final Logger log = LoggerFactory.getLogger(DiscordWebhookSupport.class);

    private DiscordWebhookSupport() {}

    public static boolean isConfigured(String webhookId, String webhookToken) {
        return StringUtils.hasText(webhookId) && StringUtils.hasText(webhookToken);
    }

    public static void sendSafe(
            DiscordApiClient client, String webhookId, String webhookToken, String content) {
        if (client == null || !isConfigured(webhookId, webhookToken)) {
            log.debug("Discord webhook skipped (not configured)");
            return;
        }
        try {
            DiscordWebhookMessage message =
                    DiscordWebhookMessage.builder().content(content).build();
            RetrofitCallHandler._void_execute(client.sendWebhookMessage(message, webhookId, webhookToken));
        } catch (Exception e) {
            log.warn("Discord webhook failed (ignored): {}", e.getMessage());
        }
    }

    public static void sendSafe(
            DiscordApiClient client,
            String webhookId,
            String webhookToken,
            String username,
            String content) {
        if (client == null || !isConfigured(webhookId, webhookToken)) {
            log.debug("Discord webhook skipped (not configured)");
            return;
        }
        try {
            DiscordWebhookMessage message = DiscordWebhookMessage.builder()
                    .username(username)
                    .content(content)
                    .build();
            RetrofitCallHandler._void_execute(client.sendWebhookMessage(message, webhookId, webhookToken));
        } catch (Exception e) {
            log.warn("Discord webhook failed (ignored): {}", e.getMessage());
        }
    }
}
