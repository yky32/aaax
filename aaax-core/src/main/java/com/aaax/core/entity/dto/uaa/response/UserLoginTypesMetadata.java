package com.aaax.core.entity.dto.uaa.response;

import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.entity.dto.BaseResponseDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuperBuilder
public class UserLoginTypesMetadata extends BaseResponseDto {
    private String userId;
    private String username;
    private LoginType loginType;
    private UserStatus status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "en_US", timezone = "UTC")
    private Instant lastLoginDt;
}
