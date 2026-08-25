package com.aaax.core.kafka.event;

import com.aaax.core.common.jsonfield.LogContextMetadata;
import com.aaax.core.common.jsonfield.RequestContextMetadata;
import com.aaax.core.kafka.BaseEvent;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogCreatedEvent extends BaseEvent {
    private RequestContextMetadata requestContext;
    private LogContextMetadata logContext;
}
