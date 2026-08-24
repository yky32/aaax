package com.aaax.usecase.otp;

import com.aaax.core.constant.enu.Locale;
import com.aaax.core.constant.enu.NotificationAction;
import com.aaax.core.constant.enu.NotificationChannel;
import com.aaax.core.constant.enu.NotificationFrequency;
import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.BaseNotificationEvent;
import com.aaax.core.kafka.CreateMassNotificationRequestDto;
import com.aaax.core.kafka.enu.KafkaTopic;
import com.aaax.core.utils.*;
import com.aaax.config.redis.RedisKey;
import com.aaax.entity.dto.json_context.OtpMetadata;
import com.aaax.entity.dto.json_context.SystemConfigMetadata;
import com.aaax.entity.dto.request.CreateOtpRequestDto;
import com.aaax.entity.dto.request.VerifyOtpRequestDto;
import com.aaax.entity.dto.response.GetSystemConfigurationRequestDto;
import com.aaax.entity.enu.OtpSystemConfigTarget;
import com.aaax.entity.enu.OtpType;
import com.aaax.entity.po.configuration.SystemConfiguration;
import com.aaax.exception.response.OtpErrorResponse;
import com.aaax.usecase.SystemConfigurationUseCase;
import com.aaax.usecase.intf.OtpHandler;
import com.aaax.utils.OtpUtil;
import com.aaax.validation.UaaValidation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class OtpUseCase implements OtpHandler<OtpMetadata, CreateOtpRequestDto> {
    protected final KafkaUtil kafkaUtil;
    protected final RedisUtil redisUtil;
    protected final SystemConfigurationUseCase systemConfigurationUseCase;
    @Value("${config.system-invoker}")
    protected String systemInvoker;
    @Value("${notification-template.usecase.otp-register.email}")
    protected String otpRegisterTemplate;
    @Value("${notification-template.rentease.usecase.otp-register.email}")
    protected String renteaseOtpRegisterTemplate;

    protected static @NotNull String getRedisKey(CreateOtpRequestDto dto) {
        String to = UaaValidation.toCanonicalIdentifierIfPresent(dto.getTo());
        if (StringUtils.isNotBlank(to)) {
            dto.setTo(to);
        }
        String key = dto.getUsecase().getKey().concat(dto.getTo());
        log.info("-- RedisKey => [{}]", key);
        return key;
    }

    public OtpMetadata re_generate(CreateOtpRequestDto dto) {
        return this.generate(dto, "RE_GENERATE");
    }

    /**
     * @param dto       OTP Request dto.
     * @param whereFrom where this generate method is called from.
     */
    public OtpMetadata generate(CreateOtpRequestDto dto, String whereFrom) {
        log.info("-- Calling from {}, generate OTP => {}", whereFrom, dto);

        // === assumption ===
        dto.setIsPush(Optional.ofNullable(dto.getIsPush()).orElse(true));
        dto.setIsOverride(Optional.ofNullable(dto.getIsOverride()).orElse(false));

        String redisKey = getRedisKey(dto);
        SystemConfigMetadata systemConfig = Optional.ofNullable(dto.getSystemConfig())
                .orElse(SystemConfigMetadata.builder()
                        .target(OtpSystemConfigTarget.OTP_TTL)
                        .scope("GLOBAL")
                        .build());
        GetSystemConfigurationRequestDto config =
                systemConfigurationUseCase.query(systemConfig.getTarget(), systemConfig.getScope());

        log.info("==================================================================");
        log.info("====== target: [{}] ======= scope: [{}] ======= liveTtlSec: [{}] ========",
                config.getTarget(), config.getScope(), config.getValue());
        log.info("==================================================================");

        // Resend cooldown (independent of live TTL)
        GetSystemConfigurationRequestDto intervalConfig =
                systemConfigurationUseCase.query(OtpSystemConfigTarget.OTP_RESEND_TTL, "GLOBAL");
        int intervalTtl = Integer.parseInt(String.valueOf(intervalConfig.getValue()));

        // === validations ===
        this.mustValidations(dto.getUsecase(), dto.getTo());
        // === validations ===

        if (!dto.getIsOverride()) {
            if (redisUtil.hasKey(OtpUtil.markAsGenerated(redisKey))) {
                throw new BizException(OtpErrorResponse.OTP0429, Map.of(
                        "message", "Sorry. Don't try me so hard please. =>".concat(String.valueOf(intervalTtl)),
                        "otpLiveTime", redisUtil.getExpire(redisKey),
                        "intervalTime", redisUtil.getExpire(OtpUtil.markAsGenerated(redisKey))
                ));
            }
        }
        // === validations ===

        // === otp ===
        OtpMetadata otpMetadata = this.validationRecordInRedis(dto);
        // === otp ===

        redisUtil.set(OtpUtil.markAsGenerated(redisKey), InstantUtil.parse(Instant.now()), intervalTtl);
        this.setRedis(redisKey, otpMetadata, Integer.parseInt(String.valueOf(config.getValue())));

        // ==== kafka produce ====
        if (dto.getIsPush()) {
            Map<String, Object> notificationParam = new HashMap();
            // get the sender from db if set otherwise use default sender from noti
            Optional<SystemConfiguration> senderSystemConfiguration = systemConfigurationUseCase.getOptionalSystemConfig("EMAIL_SENDER", dto.getSourceSystem());
            senderSystemConfiguration.ifPresent(systemConfiguration -> notificationParam.put("from", String.valueOf(systemConfiguration.getValue())));
            notificationParam.put("name", dto.getTo());
            notificationParam.put("otp", otpMetadata.getCode());
            notificationParam.put("email", dto.getTo());
            if (Optional.ofNullable(dto.getTemplateId()).isPresent()) {
                notificationParam.put("templateId", dto.getTemplateId());
            }

            CreateMassNotificationRequestDto event = CreateMassNotificationRequestDto.builder()
                    .channels(List.of(NotificationChannel.EMAIL))   // only send email first
                    .parameterMap(Optional.ofNullable(dto.getMetadata()).isPresent() ? DtoUtil.partialUpdateBuilderToMap(notificationParam, dto.getMetadata()) : notificationParam)
                    .locale(List.of(Locale.EN))
                    .systemInvoker(systemInvoker)
                    .notificationAction(NotificationAction.REALTIME)
                    .notificationFrequency(NotificationFrequency.ONE_OFF)
                    .requestId(UUID.randomUUID().toString())
                    .recipients(List.of(BaseNotificationEvent.builder()
                            .to(dto.getTo())
                            .build()))
                    .build();
            this.triggerNotification(event, dto.getNotificationTemplate());
        }
        // ==== kafka produce ====
        return otpMetadata;
    }

    @Override
    public void mustValidations(RedisKey usecase, String to) {
        // === validations ===
        OtpUtil.isValidKey(usecase);
        OtpUtil.isValidRecipient(to);
        // === validations ===
    }

    @Override
    public OtpMetadata validationRecordInRedis(CreateOtpRequestDto dto) {
        String otpCode = OtpUtil.generate(
                Optional.ofNullable(dto.getDigit()).orElse(6),
                Optional.ofNullable(dto.getType()).orElse(OtpType.DIGIT)
        );
        return OtpMetadata.builder()
                .code(otpCode)
                .counter(0)
                .build();
    }

    @Override
    public OtpMetadata convertRedisRecordTo(String redisKey) {
        return JSONUtil.convertFromObject(redisUtil.get(redisKey), OtpMetadata.class);
    }


    @Override
    public void setRedis(String redisKey, OtpMetadata otpMetadata, int ttl) {
        // separate to 2 2 different ttl for live time & interval time
//        redisUtil.set(OtpUtil.markAsGenerated(redisKey), InstantUtil.parse(Instant.now()), ttl);
        redisUtil.set(redisKey, otpMetadata, ttl);

        // return back the ttl to frontend side only
        otpMetadata.setTtl(ttl);
    }


    protected int getExpiredTtl(String redisKey) {
        return Math.toIntExact(redisUtil.getExpire(redisKey));
    }

    public boolean verify(VerifyOtpRequestDto dto) {
        if (StringUtils.isNotBlank(dto.getTo())) {
            dto.setTo(UaaValidation.toCanonicalIdentifier(dto.getTo()));
        }
        this.mustValidations(dto.getUsecase(), dto.getTo());
        if (StringUtils.isBlank(dto.getCode())) {
            throw new BizException(OtpErrorResponse.OTP0400, "[code] was null/empty in request.");
        }

        String redisKey = dto.getUsecase().getKey().concat(dto.getTo());
        if (!redisUtil.hasKey(redisKey)) {
            throw new BizException(OtpErrorResponse.OTP0001, "No key. => ".concat(dto.getTo()));
        }
        OtpMetadata otpMetadata = this.convertRedisRecordTo(redisKey);
        switch (otpMetadata.getCounter()) {
            case (3) ->
                    throw new BizException(OtpErrorResponse.OTP0002, "Sorry. Too many failed request. => ".concat(String.valueOf(otpMetadata.getCounter())));
        }
        if (!dto.getCode().equals(otpMetadata.getCode())) {
            otpMetadata.setCounter(otpMetadata.getCounter() + 1);
            long remainTtl = redisUtil.getExpire(redisKey);
            redisUtil.set(redisKey, otpMetadata, remainTtl);
            throw new BizException(OtpErrorResponse.OTP0002, "Invalid for => ".concat(dto.getUsecase().getFeature()));
        }

        // ==== success case =====
        this.successAndCleanupRedis(redisKey);
        return true;
    }

    @Override
    public void successAndCleanupRedis(String redisKey) {
        redisUtil.delete(redisKey);
        redisUtil.delete(OtpUtil.markAsGenerated(redisKey));
    }

    @Override
    public void triggerNotification(BaseNotificationEvent event) {
        log.info("-- doTriggerNotification => {}", event);
        try {
            kafkaUtil.send(KafkaTopic.NOTIFICATION_MASS, event);
        } catch (Exception exception) {
            log.info("-- doTriggerNotification Error => {}", event);
        }
        log.info("-- doTriggerNotification END => {}", event);
    }

    @Override
    public void triggerNotification(BaseNotificationEvent event, String templateName) {
        event.setNotificationTemplateName(templateName);
        this.triggerNotification(event);
    }

    private String getTemplateIdBySourceSystem(String sourceSystem) {
        switch (sourceSystem) {
            case "RENTEASE" -> {
                return renteaseOtpRegisterTemplate;
            }
            default -> {
                return otpRegisterTemplate;
            }
        }
    }

    public Object queryBackTheStoredValueInRedis(String redisKey) {
        return null;
    }

    public boolean isVerifiedAlready(String username) {
        return false;
    }
}
