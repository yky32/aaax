package com.aaax.core.kafka.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.aaax.core.kafka.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostLoginSucceedEvent extends BaseEvent {
    private String domain;
    private String event;
    private String userId;
    private Instant startTrafficDt;
    private Instant endTrafficDt;

    private Object requestBody; // log content
    private Object responseBody; // JWT (accessToken, refreshToken)
}
