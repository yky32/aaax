package com.aaax.core.kafka.event;

import com.aaax.core.kafka.BaseEvent;
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
public class UserClickedEvent extends BaseEvent {
    private String key; // profile
    private String value; // profile id
}
