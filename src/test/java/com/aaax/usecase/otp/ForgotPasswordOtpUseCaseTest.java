package com.aaax.usecase.otp;

import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.BaseNotificationEvent;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.config.redis.RedisKey;
import com.aaax.entity.dto.json_context.OtpMetadata;
import com.aaax.entity.dto.json_context.RegisterOtpMetadata;
import com.aaax.entity.dto.request.CreateOtpRequestDto;
import com.aaax.entity.enu.OtpType;
import com.aaax.usecase.SystemConfigurationUseCase;
import com.aaax.utils.OtpUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordOtpUseCaseTest {

    @Mock
    private KafkaUtil kafkaUtil;
    @Mock
    private RedisUtil redisUtil;
    @Mock
    private SystemConfigurationUseCase systemConfigurationUseCase;

    @InjectMocks
    private ForgotPasswordOtpUseCase forgotPasswordOtpUseCase;

    @Test
    @DisplayName("re_generate should throw when not occupied")
    void reGenerate_shouldThrowWhenNotOccupied() {
        CreateOtpRequestDto dto = CreateOtpRequestDto.builder()
                .to("user@test.com")
                .usecase(RedisKey.OTP_RESET_PASSWORD)
                .build();
        when(redisUtil.hasKey(anyString())).thenReturn(false);

        assertThrows(BizException.class, () -> forgotPasswordOtpUseCase.re_generate(dto));
    }

    @Test
    @DisplayName("re_generate should return existing OTP when key present")
    void reGenerate_shouldReturnExisting() {
        CreateOtpRequestDto dto = CreateOtpRequestDto.builder()
                .to("user@test.com")
                .usecase(RedisKey.OTP_RESET_PASSWORD)
                .build();
        String key = RedisKey.OTP_RESET_PASSWORD.getKey().concat("user@test.com");
        when(redisUtil.hasKey(OtpUtil.markAs(key, ":isOccupied"))).thenReturn(true);
        when(redisUtil.hasKey(key)).thenReturn(true);
        when(redisUtil.get(key)).thenReturn(OtpMetadata.builder().code("111111").counter(0).build());
        when(redisUtil.getExpire(key)).thenReturn(90L);

        OtpMetadata result = forgotPasswordOtpUseCase.re_generate(dto);

        assertEquals("111111", result.getCode());
        assertEquals(90, result.getTtl());
    }

    @Test
    @DisplayName("markAsOccupied and isVerifiedAlready should use redis")
    void markAndVerified_shouldUseRedis() {
        forgotPasswordOtpUseCase.markAsOccupied("otp:reset-password:user@test.com");
        verify(redisUtil).set(contains(":isOccupied"), any(), eq(1800L));

        when(redisUtil.hasKey(OtpUtil.markAsVerified(RedisKey.OTP_RESET_PASSWORD.getKey().concat("user@test.com"))))
                .thenReturn(true);
        assertTrue(forgotPasswordOtpUseCase.isVerifiedAlready("user@test.com"));
    }

    @Test
    @DisplayName("queryBackTheStoredValueInRedis should return ttl metadata")
    void queryBack_shouldReturnTtl() {
        when(redisUtil.getExpire(anyString())).thenReturn(120L);
        OtpMetadata metadata = forgotPasswordOtpUseCase.queryBackTheStoredValueInRedis("user@test.com");
        assertEquals(120, metadata.getTtl());
    }

    @Test
    @DisplayName("successAndCleanupRedis should mark verified then cleanup")
    void successAndCleanup_shouldMarkVerified() {
        String redisKey = "otp:reset-password:user@test.com";
        when(redisUtil.get(redisKey)).thenReturn(RegisterOtpMetadata.builder()
                .code("123456").counter(0).isVerified(false).build());

        forgotPasswordOtpUseCase.successAndCleanupRedis(redisKey);

        verify(redisUtil).set(eq(OtpUtil.markAsVerified(redisKey)), any(RegisterOtpMetadata.class), eq(1800L));
        verify(redisUtil).delete(redisKey);
        verify(redisUtil).delete(OtpUtil.markAsGenerated(redisKey));
    }

    @Test
    @DisplayName("validationRecordInRedis should build register metadata")
    void validationRecord_shouldBuildMetadata() {
        RegisterOtpMetadata metadata = forgotPasswordOtpUseCase.validationRecordInRedis(
                CreateOtpRequestDto.builder().digit(6).type(OtpType.DIGIT).build());
        assertNotNull(metadata.getCode());
        assertEquals(0, metadata.getCounter());
        assertFalse(metadata.getIsVerified());
    }

    @Test
    @DisplayName("triggerNotification should force otp.user-register template")
    void triggerNotification_shouldForceTemplate() {
        BaseNotificationEvent event = mock(BaseNotificationEvent.class);
        forgotPasswordOtpUseCase.triggerNotification(event, "ignored");
        verify(event).setNotificationTemplateName("otp.user-register");
        verify(kafkaUtil).send(any(), eq(event));
    }
}
