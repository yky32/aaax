package com.aaax.core.kafka.event;

import com.aaax.core.kafka.BaseNotificationEvent;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpGeneratedEvent extends BaseNotificationEvent {
    private String to;
    private String code;
}
