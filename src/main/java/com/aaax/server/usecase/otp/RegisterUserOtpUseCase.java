package com.aaax.server.usecase.otp;

import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.BaseNotificationEvent;
import com.aaax.core.utils.InstantUtil;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.entity.dto.json_context.OtpMetadata;
import com.aaax.server.entity.dto.json_context.RegisterOtpMetadata;
import com.aaax.server.entity.dto.request.CreateOtpRequestDto;
import com.aaax.server.entity.dto.request.VerifyOtpRequestDto;
import com.aaax.server.entity.dto.response.GetSystemConfigurationRequestDto;
import com.aaax.server.entity.enu.OtpType;
import com.aaax.server.entity.po.configuration.SystemConfiguration;
import com.aaax.server.exception.response.OtpErrorResponse;
import com.aaax.server.usecase.SystemConfigurationUseCase;
import com.aaax.server.utils.OtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class RegisterUserOtpUseCase extends OtpUseCase {

    private final String OCCUPIED_ACTION = ":isOccupied";

    public RegisterUserOtpUseCase(KafkaUtil kafkaUtil, RedisUtil redisUtil, SystemConfigurationUseCase systemConfigurationUseCase, RedisUtil redisUtil1) {
        super(kafkaUtil, redisUtil, systemConfigurationUseCase);
    }


    @Override
    public OtpMetadata re_generate(CreateOtpRequestDto dto) {
        // ==== checking for re-generate
        String userRegisterRedisKey = getRedisKey(dto);

        if (!redisUtil.hasKey(OtpUtil.markAs(userRegisterRedisKey, OCCUPIED_ACTION))) {
            throw new BizException(OtpErrorResponse.OTP0003);
        }

        // === if within 60-s => return current one. early return
        if (this.redisUtil.hasKey(OtpUtil.markAsGenerated(userRegisterRedisKey))) {
            throw new BizException(OtpErrorResponse.OTP0429, Map.of(
                    "message", "Sorry. Don't try me so hard please. =>".concat(String.valueOf(redisUtil.getExpire(OtpUtil.markAsGenerated(userRegisterRedisKey)))),
                    "otpLiveTime", redisUtil.getExpire(userRegisterRedisKey),
                    "intervalTime", redisUtil.getExpire(OtpUtil.markAsGenerated(userRegisterRedisKey))
            ));
        }

        dto.setIsOverride(true);
        return super.re_generate(dto);
    }

    public void markAsOccupied(String redisKey) {
        redisUtil.set(OtpUtil.markAs(redisKey, OCCUPIED_ACTION),  InstantUtil.parse(Instant.now()), 1800);
    }

    @Override
    public boolean verify(VerifyOtpRequestDto dto) {
        return super.verify(dto);
    }

    @Override
    public void triggerNotification(BaseNotificationEvent event, String templateName) {
        // fetch from system configuration if wanna override.
        String notificationTemplate = "otp.user-register";
        Optional<SystemConfiguration> config = systemConfigurationUseCase.getOptionalSystemConfig("OTP_USR_REGISTER_NOTIFICATION_TEMPLATE", "GLOBAL");
        if (config.isPresent()) {
            notificationTemplate = String.valueOf(config.get().getValue());
        }
        super.triggerNotification(event, notificationTemplate);
    }

    @Override
    public RegisterOtpMetadata convertRedisRecordTo(String redisKey) {
        return JSONUtil.convertFromObject(redisUtil.get(redisKey), RegisterOtpMetadata.class);
    }

    @Override
    public RegisterOtpMetadata validationRecordInRedis(CreateOtpRequestDto dto) {
        String otpCode = OtpUtil.generate(
                Optional.ofNullable(dto.getDigit()).orElse(6),
                Optional.ofNullable(dto.getType()).orElse(OtpType.DIGIT)
        );
        return RegisterOtpMetadata.builder()
                .code(otpCode)
                .counter(0)
                .isVerified(false)
                .build();
    }

    @Override
    public void successAndCleanupRedis(String redisKey) {
        RegisterOtpMetadata registerOtpMetadata = JSONUtil.convertFromObject(redisUtil.get(redisKey), RegisterOtpMetadata.class);
        // === mark as [isVerified]
        registerOtpMetadata.setIsVerified(true);
        redisUtil.set(OtpUtil.markAsVerified(redisKey), registerOtpMetadata, 1800);

        // === its verified... remove the [generated] key and [original] key
        super.successAndCleanupRedis(redisKey);
    }

}
