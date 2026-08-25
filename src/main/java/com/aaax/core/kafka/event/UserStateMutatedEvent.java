package com.aaax.core.kafka.event;

import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.kafka.BaseEvent;
import com.aaax.core.security.AuditAwareUser;
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
public class UserStateMutatedEvent extends BaseEvent {
    private String userId;
    private String username;
    private String action;
    private UserStatus toBeStatus;
    private Integer currentVersion;
    private AuditAwareUser actionBy;
}
