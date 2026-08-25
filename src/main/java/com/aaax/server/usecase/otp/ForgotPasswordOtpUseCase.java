package com.aaax.server.usecase.otp;

import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.BaseNotificationEvent;
import com.aaax.core.utils.InstantUtil;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.config.redis.RedisKey;
import com.aaax.server.entity.dto.json_context.OtpMetadata;
import com.aaax.server.entity.dto.json_context.RegisterOtpMetadata;
import com.aaax.server.entity.dto.request.CreateOtpRequestDto;
import com.aaax.server.entity.dto.request.VerifyOtpRequestDto;
import com.aaax.server.entity.enu.OtpType;
import com.aaax.server.exception.response.OtpErrorResponse;
import com.aaax.server.usecase.SystemConfigurationUseCase;
import com.aaax.server.utils.OtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@Slf4j
public class ForgotPasswordOtpUseCase extends OtpUseCase {

    private final String OCCUPIED_ACTION = ":isOccupied";
    private final RedisKey REDIS_KEY = RedisKey.OTP_RESET_PASSWORD;

    public ForgotPasswordOtpUseCase(KafkaUtil kafkaUtil, RedisUtil redisUtil, SystemConfigurationUseCase systemConfigurationUseCase, RedisUtil redisUtil1) {
        super(kafkaUtil, redisUtil, systemConfigurationUseCase);
    }

    @Override
    public OtpMetadata re_generate(CreateOtpRequestDto dto) {
        // ==== checking for re-generate
        String userRegisterRedisKey = getRedisKey(dto);

        if (!redisUtil.hasKey(_isOccupiedRedisKey(userRegisterRedisKey))) {
            throw new BizException(OtpErrorResponse.OTP0003);
        }

        // === if within 60-s => return current one. early return
        if (this.redisUtil.hasKey(userRegisterRedisKey)) {
            OtpMetadata otpMetadata = JSONUtil.convertFromObject(redisUtil.get(userRegisterRedisKey), OtpMetadata.class);
            otpMetadata.setTtl(super.getExpiredTtl(userRegisterRedisKey)); // == get back latest ttl
            return otpMetadata;
        }

        dto.setIsOverride(true);
        return super.re_generate(dto);
    }

    public void markAsOccupied(String redisKey) {
        redisUtil.set(_isOccupiedRedisKey(redisKey), InstantUtil.parse(Instant.now()), _ttl());
    }

    private int _ttl() {
        // TODO: can be configured from DB
        return 1800; // in seconds
    }

    @NotNull
    private String _isOccupiedRedisKey(String redisKey) {
        return OtpUtil.markAs(redisKey, OCCUPIED_ACTION);
    }


    @Override
    public boolean isVerifiedAlready(String input) {
        String redisKey = OtpUtil.markAsVerified(REDIS_KEY.getKey().concat(input));
        return redisUtil.hasKey(redisKey);
    }

    @Override
    @NotNull
    public OtpMetadata queryBackTheStoredValueInRedis(String input) {
        String redisKey = this._isOccupiedRedisKey(REDIS_KEY.getKey().concat(input));
        return OtpMetadata.builder()
                .ttl(super.getExpiredTtl(redisKey))
                .build();
    }

    @Override
    public boolean verify(VerifyOtpRequestDto dto) {
        return super.verify(dto);
    }

    @Override
    public void triggerNotification(BaseNotificationEvent event, String templateName) {
        super.triggerNotification(event, "otp.user-register");
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
        redisUtil.set(OtpUtil.markAsVerified(redisKey), registerOtpMetadata, _ttl());

        // === its verified... remove the [generated] key and [original] key
        super.successAndCleanupRedis(redisKey);
    }

}
