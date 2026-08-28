package com.aaax.server.usecase.otp;

import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.BaseNotificationEvent;
import com.aaax.core.kafka.CreateMassNotificationRequestDto;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.config.redis.RedisKey;
import com.aaax.server.entity.dto.json_context.OtpMetadata;
import com.aaax.server.entity.dto.request.CreateOtpRequestDto;
import com.aaax.server.entity.dto.request.VerifyOtpRequestDto;
import com.aaax.server.entity.dto.response.GetSystemConfigurationRequestDto;
import com.aaax.server.entity.enu.OtpType;
import com.aaax.server.entity.po.configuration.SystemConfiguration;
import com.aaax.server.usecase.SystemConfigurationUseCase;
import com.aaax.server.utils.OtpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpUseCaseTest {

    @Mock
    private KafkaUtil kafkaUtil;
    @Mock
    private RedisUtil redisUtil;
    @Mock
    private SystemConfigurationUseCase systemConfigurationUseCase;

    @InjectMocks
    private OtpUseCase otpUseCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpUseCase, "systemInvoker", "aaax");
        ReflectionTestUtils.setField(otpUseCase, "otpRegisterTemplate", "otp.user-register");
        ReflectionTestUtils.setField(otpUseCase, "otpRegisterTemplate", "otp.register");
    }

    private CreateOtpRequestDto baseDto() {
        return CreateOtpRequestDto.builder()
                .to("user@test.com")
                .usecase(RedisKey.OTP_GENERAL)
                .type(OtpType.DIGIT)
                .digit(6)
                .sourceSystem("APP")
                .isPush(true)
                .isOverride(true)
                .build();
    }

    @Test
    @DisplayName("generate should create OTP, store redis, and push notification")
    void generate_shouldCreateAndPush() {
        when(systemConfigurationUseCase.query("OTP_TTL", "GLOBAL"))
                .thenReturn(GetSystemConfigurationRequestDto.builder().target("OTP_TTL").scope("GLOBAL").value(300).build());
        when(systemConfigurationUseCase.query("OTP_RESEND_TTL", "GLOBAL"))
                .thenReturn(GetSystemConfigurationRequestDto.builder().value(60).build());
        when(systemConfigurationUseCase.getOptionalSystemConfig("EMAIL_SENDER", "APP"))
                .thenReturn(Optional.of(SystemConfiguration.builder().value("noreply@test.com").build()));

        OtpMetadata result = otpUseCase.generate(baseDto(), "TEST");

        assertNotNull(result.getCode());
        assertEquals(6, result.getCode().length());
        assertEquals(300, result.getTtl());
        verify(redisUtil, atLeastOnce()).set(anyString(), any(), anyLong());
        verify(kafkaUtil).send(any(), any());
    }

    @Test
    @DisplayName("generate should throw when interval key exists and override false")
    void generate_shouldThrowWhenIntervalActive() {
        CreateOtpRequestDto dto = baseDto();
        dto.setIsOverride(false);
        when(systemConfigurationUseCase.query("OTP_TTL", "GLOBAL"))
                .thenReturn(GetSystemConfigurationRequestDto.builder().value(300).build());
        when(systemConfigurationUseCase.query("OTP_RESEND_TTL", "GLOBAL"))
                .thenReturn(GetSystemConfigurationRequestDto.builder().value(60).build());
        when(redisUtil.hasKey(OtpUtil.markAsGenerated(RedisKey.OTP_GENERAL.getKey().concat("user@test.com"))))
                .thenReturn(true);
        when(redisUtil.getExpire(anyString())).thenReturn(30L);

        assertThrows(BizException.class, () -> otpUseCase.generate(dto, "TEST"));
    }

    @Test
    @DisplayName("generate should skip push when isPush false")
    void generate_shouldSkipPush() {
        CreateOtpRequestDto dto = baseDto();
        dto.setIsPush(false);
        when(systemConfigurationUseCase.query("OTP_TTL", "GLOBAL"))
                .thenReturn(GetSystemConfigurationRequestDto.builder().value(120).build());
        when(systemConfigurationUseCase.query("OTP_RESEND_TTL", "GLOBAL"))
                .thenReturn(GetSystemConfigurationRequestDto.builder().value(60).build());

        OtpMetadata result = otpUseCase.generate(dto, "TEST");

        assertNotNull(result.getCode());
        verify(kafkaUtil, never()).send(any(), any());
    }

    @Test
    @DisplayName("re_generate should delegate to generate")
    void reGenerate_shouldDelegate() {
        when(systemConfigurationUseCase.query("OTP_TTL", "GLOBAL"))
                .thenReturn(GetSystemConfigurationRequestDto.builder().value(120).build());
        when(systemConfigurationUseCase.query("OTP_RESEND_TTL", "GLOBAL"))
                .thenReturn(GetSystemConfigurationRequestDto.builder().value(60).build());
        CreateOtpRequestDto dto = baseDto();
        dto.setIsPush(false);

        assertNotNull(otpUseCase.re_generate(dto).getCode());
    }

    @Test
    @DisplayName("verify should succeed for correct code")
    void verify_shouldSucceed() {
        VerifyOtpRequestDto dto = VerifyOtpRequestDto.builder()
                .to("user@test.com")
                .code("123456")
                .usecase(RedisKey.OTP_GENERAL)
                .build();
        String redisKey = RedisKey.OTP_GENERAL.getKey().concat("user@test.com");
        when(redisUtil.hasKey(redisKey)).thenReturn(true);
        when(redisUtil.get(redisKey)).thenReturn(OtpMetadata.builder().code("123456").counter(0).build());

        assertTrue(otpUseCase.verify(dto));
        verify(redisUtil).delete(redisKey);
        verify(redisUtil).delete(OtpUtil.markAsGenerated(redisKey));
    }

    @Test
    @DisplayName("verify should throw when code blank")
    void verify_shouldThrowWhenCodeBlank() {
        VerifyOtpRequestDto dto = VerifyOtpRequestDto.builder()
                .to("user@test.com")
                .code(" ")
                .usecase(RedisKey.OTP_GENERAL)
                .build();
        assertThrows(BizException.class, () -> otpUseCase.verify(dto));
    }

    @Test
    @DisplayName("verify should throw when redis key missing")
    void verify_shouldThrowWhenMissingKey() {
        VerifyOtpRequestDto dto = VerifyOtpRequestDto.builder()
                .to("user@test.com")
                .code("123456")
                .usecase(RedisKey.OTP_GENERAL)
                .build();
        when(redisUtil.hasKey(anyString())).thenReturn(false);
        assertThrows(BizException.class, () -> otpUseCase.verify(dto));
    }

    @Test
    @DisplayName("verify should throw and increment counter on wrong code")
    void verify_shouldThrowOnWrongCode() {
        VerifyOtpRequestDto dto = VerifyOtpRequestDto.builder()
                .to("user@test.com")
                .code("000000")
                .usecase(RedisKey.OTP_GENERAL)
                .build();
        String redisKey = RedisKey.OTP_GENERAL.getKey().concat("user@test.com");
        when(redisUtil.hasKey(redisKey)).thenReturn(true);
        when(redisUtil.get(redisKey)).thenReturn(OtpMetadata.builder().code("123456").counter(1).build());
        when(redisUtil.getExpire(redisKey)).thenReturn(100L);

        assertThrows(BizException.class, () -> otpUseCase.verify(dto));
        verify(redisUtil).set(eq(redisKey), any(OtpMetadata.class), eq(100L));
    }

    @Test
    @DisplayName("verify should throw when counter is 3")
    void verify_shouldThrowWhenTooManyAttempts() {
        VerifyOtpRequestDto dto = VerifyOtpRequestDto.builder()
                .to("user@test.com")
                .code("123456")
                .usecase(RedisKey.OTP_GENERAL)
                .build();
        String redisKey = RedisKey.OTP_GENERAL.getKey().concat("user@test.com");
        when(redisUtil.hasKey(redisKey)).thenReturn(true);
        when(redisUtil.get(redisKey)).thenReturn(OtpMetadata.builder().code("123456").counter(3).build());

        assertThrows(BizException.class, () -> otpUseCase.verify(dto));
    }

    @Test
    @DisplayName("mustValidations should reject non-OTP redis key")
    void mustValidations_shouldRejectBadKey() {
        assertThrows(BizException.class, () -> otpUseCase.mustValidations(RedisKey.USER_OAUTH_TOKENS, "user@test.com"));
    }

    @Test
    @DisplayName("triggerNotification should swallow kafka errors")
    void triggerNotification_shouldSwallowErrors() {
        doThrow(new RuntimeException("kafka down")).when(kafkaUtil).send(any(), any());
        assertDoesNotThrow(() -> otpUseCase.triggerNotification(CreateMassNotificationRequestDto.builder().build()));
    }

    @Test
    @DisplayName("queryBackTheStoredValueInRedis and isVerifiedAlready defaults")
    void defaults_shouldReturnNullAndFalse() {
        assertNull(otpUseCase.queryBackTheStoredValueInRedis("any"));
        assertFalse(otpUseCase.isVerifiedAlready("any"));
    }
}
