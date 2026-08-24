package com.aaax.core.kafka.event.log;

import com.aaax.core.constant.enu.NotificationExecutionStatus;
import com.aaax.core.entity.dto.notification.TemplateSystemControlMetadata;
import com.aaax.core.kafka.BaseEvent;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationLogCreatedEvent extends BaseEvent {
    private String executionId;
    // == po fields
    private NotificationExecutionStatus status;
    private String actionBy;
    private Object content;
    private Object metadata;
    private String correlationId;
    private String traceId;
    private String userId;
    private List<TemplateSystemControlMetadata> systemControl;
}
