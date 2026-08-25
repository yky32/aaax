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
public class UserAliasGeneratedEvent extends BaseEvent {
    private String userId;
    private String username;
    private String idGeneratorKey; // the key to query for DB
}
