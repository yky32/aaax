package com.aaax.core.kafka.event;

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
public class UserCreatedEvent extends BaseEvent {
    private Long userId;
    private String username;
    private List<String> sourceSystems;
}
