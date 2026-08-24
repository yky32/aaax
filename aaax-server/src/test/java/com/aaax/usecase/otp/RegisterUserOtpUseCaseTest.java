package com.aaax.usecase.otp;

import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.BaseNotificationEvent;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.config.redis.RedisKey;
import com.aaax.entity.dto.json_context.RegisterOtpMetadata;
import com.aaax.entity.dto.request.CreateOtpRequestDto;
import com.aaax.entity.enu.OtpType;
import com.aaax.entity.po.configuration.SystemConfiguration;
import com.aaax.usecase.SystemConfigurationUseCase;
import com.aaax.utils.OtpUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserOtpUseCaseTest {

    @Mock
    private KafkaUtil kafkaUtil;
    @Mock
    private RedisUtil redisUtil;
    @Mock
    private SystemConfigurationUseCase systemConfigurationUseCase;

    @InjectMocks
    private RegisterUserOtpUseCase registerUserOtpUseCase;

    @Test
    @DisplayName("re_generate should throw when not occupied")
    void reGenerate_shouldThrowWhenNotOccupied() {
        CreateOtpRequestDto dto = CreateOtpRequestDto.builder()
                .to("user@test.com")
                .usecase(RedisKey.OTP_USER_REGISTER)
                .build();
        when(redisUtil.hasKey(anyString())).thenReturn(false);
        assertThrows(BizException.class, () -> registerUserOtpUseCase.re_generate(dto));
    }

    @Test
    @DisplayName("re_generate should throw when generated interval key exists")
    void reGenerate_shouldThrowWhenIntervalActive() {
        CreateOtpRequestDto dto = CreateOtpRequestDto.builder()
                .to("user@test.com")
                .usecase(RedisKey.OTP_USER_REGISTER)
                .build();
        String key = RedisKey.OTP_USER_REGISTER.getKey().concat("user@test.com");
        when(redisUtil.hasKey(OtpUtil.markAs(key, ":isOccupied"))).thenReturn(true);
        when(redisUtil.hasKey(OtpUtil.markAsGenerated(key))).thenReturn(true);
        when(redisUtil.getExpire(anyString())).thenReturn(20L);

        assertThrows(BizException.class, () -> registerUserOtpUseCase.re_generate(dto));
    }

    @Test
    @DisplayName("markAsOccupied should set occupied key")
    void markAsOccupied_shouldSetKey() {
        registerUserOtpUseCase.markAsOccupied("otp:user-register:user@test.com");
        verify(redisUtil).set(contains(":isOccupied"), any(), eq(1800L));
    }

    @Test
    @DisplayName("successAndCleanupRedis should mark verified")
    void successAndCleanup_shouldMarkVerified() {
        String redisKey = "otp:user-register:user@test.com";
        when(redisUtil.get(redisKey)).thenReturn(RegisterOtpMetadata.builder()
                .code("222222").counter(0).isVerified(false).build());

        registerUserOtpUseCase.successAndCleanupRedis(redisKey);

        verify(redisUtil).set(eq(OtpUtil.markAsVerified(redisKey)), any(RegisterOtpMetadata.class), eq(1800L));
        verify(redisUtil).delete(redisKey);
    }

    @Test
    @DisplayName("validationRecordInRedis should build metadata")
    void validationRecord_shouldBuild() {
        RegisterOtpMetadata metadata = registerUserOtpUseCase.validationRecordInRedis(
                CreateOtpRequestDto.builder().digit(4).type(OtpType.DIGIT).build());
        assertEquals(4, metadata.getCode().length());
        assertFalse(metadata.getIsVerified());
    }

    @Test
    @DisplayName("triggerNotification should use configured template when present")
    void triggerNotification_shouldUseConfiguredTemplate() {
        when(systemConfigurationUseCase.getOptionalSystemConfig("OTP_USR_REGISTER_NOTIFICATION_TEMPLATE", "GLOBAL"))
                .thenReturn(Optional.of(SystemConfiguration.builder().value("otp.custom").build()));
        BaseNotificationEvent event = mock(BaseNotificationEvent.class);

        registerUserOtpUseCase.triggerNotification(event, "ignored");

        verify(event).setNotificationTemplateName("otp.custom");
        verify(kafkaUtil).send(any(), eq(event));
    }

    @Test
    @DisplayName("triggerNotification should fallback to default template")
    void triggerNotification_shouldFallback() {
        when(systemConfigurationUseCase.getOptionalSystemConfig("OTP_USR_REGISTER_NOTIFICATION_TEMPLATE", "GLOBAL"))
                .thenReturn(Optional.empty());
        BaseNotificationEvent event = mock(BaseNotificationEvent.class);

        registerUserOtpUseCase.triggerNotification(event, "ignored");

        verify(event).setNotificationTemplateName("otp.user-register");
    }
}
