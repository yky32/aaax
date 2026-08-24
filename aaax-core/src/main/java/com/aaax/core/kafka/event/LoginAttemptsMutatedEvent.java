package com.aaax.core.kafka.event;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.aaax.core.kafka.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginAttemptsMutatedEvent extends BaseEvent {
    private String userId;
    private String username;
    private Boolean isSuccess;

    private Object requestBody; // log content
    private Object responseBody; // JWT (accessToken, refreshToken)
}
