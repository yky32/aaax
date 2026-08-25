package com.aaax.core.kafka.event.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka payload for {@link com.aaax.core.kafka.enu.KafkaTopic#NOTIFICATION_MARK_IS_ARCHIVED}: one message per user log row.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarkNotificationUserLogArchivedEvent {
    /** {@code notification_user_log.id} (e.g. prefixed {@code nul_}). */
    private String id;
    private Boolean isArchived;
}
