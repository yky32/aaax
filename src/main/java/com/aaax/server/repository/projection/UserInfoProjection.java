package com.aaax.server.repository.projection;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.constant.enu.UserStatus;
import com.aaax.server.entity.po.user.User;

import java.time.Instant;

/**
 * Projection for {@link User}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface UserInfoProjection {
    String getId();
    String getUsername();
    String getAlias();
    UserStatus getStatus();
    LoginType getLoginType();
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "en_US", timezone = "UTC")
    Instant getLastLoginDt();
    Integer getAttempts();
}