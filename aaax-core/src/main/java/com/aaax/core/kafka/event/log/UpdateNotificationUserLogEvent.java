package com.aaax.core.kafka.event.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.aaax.core.constant.enu.NotificationExecutionStatus;
import com.aaax.core.entity.dto.notification.TemplateSystemControlMetadata;
import com.aaax.core.kafka.BaseEvent;
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
public class UpdateNotificationUserLogEvent extends BaseEvent {
    /** {@code notification_user_log.id} */
    private String id;
    /** Column to patch (e.g. {@code content}). */
    private String columnName;
    /**
     * Dot path under {@code content}'s JSON object where the merge applies (each segment is one map level).
     * <ul>
     *     <li>{@code null} or blank → merge into the root content map.</li>
     *     <li>{@code parameterMap.en} → merge into {@code content.parameterMap.en} (creating missing maps).</li>
     * </ul>
     */
    private String jsonPath;
    /** Entry key written into the target map (e.g. {@code selectedOption}). */
    private String key;
    /** Entry value (e.g. selected option payload). */
    private Object value;
}
