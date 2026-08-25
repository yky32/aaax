package com.aaax.entity.dto.request;

import com.aaax.config.redis.RedisKey;
import com.aaax.entity.dto.json_context.SystemConfigMetadata;
import com.aaax.entity.enu.OtpType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateOtpRequestDto {
    private String to; // SMS = Mobile, EMAIL = Email@
    private RedisKey usecase;
    private OtpType type;
    private Integer digit;
    private Boolean isPush; // default = true;
    private Boolean isOverride; // default = false
    private SystemConfigMetadata systemConfig; // fetch back po.system_configurations
    private String notificationTemplate = "otp.general";
    private Map metadata;
    private String sourceSystem;
    private String templateId;
}
