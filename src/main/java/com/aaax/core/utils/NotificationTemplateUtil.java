package com.aaax.core.utils;

import tools.jackson.databind.JsonNode;
import com.aaax.core.api.NotificationApiClient;
import com.aaax.core.constant.enu.NotificationChannel;
import com.aaax.core.entity.dto.notification.response.GetNotificationTemplateResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared notification-template helpers for any microservice.
 * <p>
 * Channel resolution reads only {@code schema.templates[*].channels[]}, matching
 * notification-service {@code StandardNotificationListener} at send time.
 * <p>
 * Typical reuse: {@link #resolveChannels} → resolve recipients from domain context
 * → {@link NotificationUtil} realtime builders + {@code sendMass}.
 */
@Slf4j
public final class NotificationTemplateUtil {

    private NotificationTemplateUtil() {
    }

    /**
     * Fetch template by name via {@link NotificationApiClient#getByName} and parse channels from DB schema.
     * Returns an empty list when the client, template name, fetch, or schema channels are missing — no fallback channel.
     */
    public static List<NotificationChannel> resolveChannels(
            NotificationApiClient client,
            String templateName
    ) {
        if (client == null || StringUtils.isBlank(templateName)) {
            log.warn("resolveChannels: missing client or templateName={}", templateName);
            return List.of();
        }
        GetNotificationTemplateResponseDto template = RetrofitCallHandler.execute(
                client.getByName(templateName));
        List<NotificationChannel> channels = channelsFromTemplate(template);
        if (channels.isEmpty()) {
            log.warn("No channels in notification template schema name={}", templateName);
        }
        return channels;
    }

    /**
     * Parse unique valid {@link NotificationChannel} values from a template DTO's schema.
     * Returns an empty list when the DTO or schema is null/unparseable or contains no channels.
     */
    public static List<NotificationChannel> channelsFromTemplate(GetNotificationTemplateResponseDto dto) {
        if (dto == null) {
            return List.of();
        }
        return channelsFromSchema(dto.getSchema());
    }

    /**
     * Parse unique valid {@link NotificationChannel} values from {@code schema.templates[*].channels[]}.
     * Returns an empty list when schema is null/unparseable or contains no template channels.
     */
    public static List<NotificationChannel> channelsFromSchema(Object schema) {
        if (schema == null) {
            return List.of();
        }
        try {
            JsonNode root = JSONUtil.convertFromObject(schema, JsonNode.class);
            JsonNode templates = root.get("templates");
            if (templates == null || !templates.isArray() || templates.isEmpty()) {
                return List.of();
            }
            Set<String> found = new LinkedHashSet<>();
            for (JsonNode template : templates) {
                JsonNode channels = template.get("channels");
                if (channels == null || !channels.isArray()) {
                    continue;
                }
                for (JsonNode channel : channels) {
                    if (channel != null && channel.isTextual() && StringUtils.isNotBlank(channel.asText())) {
                        found.add(channel.asText().trim());
                    }
                }
            }
            List<NotificationChannel> channels = new ArrayList<>();
            for (String name : found) {
                try {
                    channels.add(NotificationChannel.get(name));
                } catch (Exception e) {
                    log.warn("Skip unknown notification channel name={} in template schema", name);
                }
            }
            return channels;
        } catch (Exception e) {
            log.warn("Failed to parse notification template schema channels: {}", e.getMessage());
            return List.of();
        }
    }
}
