package com.aaax.core.entity.dto.notification.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Mirrors notification-service {@code GetNotificationResponseDto} for Retrofit deserialization
 * of {@code GET notification-templates/name/{name}} (and similar template endpoints).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetNotificationTemplateResponseDto {

    private String id;
    private String name;
    private Object schema;
    private List<String> tags;
    private Object metadata;
    /** Soft-delete / activation flag from {@code notification_template.is_active}. */
    private Boolean isActive;
}
