package com.aaax.core.kafka;

import com.aaax.core.constant.enu.Locale;
import com.aaax.core.constant.enu.NotificationAction;
import com.aaax.core.constant.enu.NotificationChannel;
import com.aaax.core.constant.enu.NotificationFrequency;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseNotificationEvent extends BaseEvent{
    protected String notificationTemplateName;
    protected List<NotificationChannel> channels;
    //protected Map<String, String> parameterMap;
    protected Map<String, Object> parameterMap;
    protected List<Locale> locale;
    /**
     * User-facing locale for outbound delivery (e.g. {@link Locale#EN}, {@link Locale#zh_TW}).
     * When {@link #parameterMap} is locale-keyed, this is <b>required</b>: {@link #locale} lists every supported bucket
     * (e.g. {@code [en, zh]} for i18n payloads), and only {@code userLocal} is used to pick which bucket to render/send.
     * Required for standard notification sends: it selects the single outbound template locale.
     */
    protected Locale userLocal;
    protected String systemInvoker; // // PG, ATFX, PMS (define by us)
    protected String to;
    protected String from;
    // ===== controlled fields===
    protected NotificationAction notificationAction = NotificationAction.REALTIME;
    protected NotificationFrequency notificationFrequency = NotificationFrequency.ONE_OFF;
    protected String expireAt;
    protected String executeDt;
    protected String crontab;
    protected String executionId;
    protected String actionBy;
    protected DisplayConfigMetadata displayConfig;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @SuperBuilder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DisplayConfigMetadata {
        // when add new config.
        // plz add default to [false] to allow future development forget to add
        // at least wont make it dead.
        private Boolean isPopUp = false;
    }
}
