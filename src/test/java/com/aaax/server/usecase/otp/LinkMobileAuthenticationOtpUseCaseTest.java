package com.aaax.server.usecase.otp;

import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.BaseNotificationEvent;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.config.redis.RedisKey;
import com.aaax.server.entity.dto.json_context.OtpMetadata;
import com.aaax.server.entity.dto.json_context.RegisterOtpMetadata;
import com.aaax.server.entity.dto.request.CreateOtpRequestDto;
import com.aaax.server.entity.enu.OtpType;
import com.aaax.server.usecase.SystemConfigurationUseCase;
import com.aaax.server.utils.OtpUtil;
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
class LinkMobileAuthenticationOtpUseCaseTest {

    @Mock
    private KafkaUtil kafkaUtil;
    @Mock
    private RedisUtil redisUtil;
    @Mock
    private SystemConfigurationUseCase systemConfigurationUseCase;

    @InjectMocks
    private LinkMobileAuthenticationOtpUseCase linkMobileAuthenticationOtpUseCase;

    @Test
    @DisplayName("re_generate should throw when not occupied")
    void reGenerate_shouldThrowWhenNotOccupied() {
        CreateOtpRequestDto dto = CreateOtpRequestDto.builder()
                .to("852-91234567")
                .usecase(RedisKey.OTP_CUSTOM)
                .build();
        when(redisUtil.hasKey(anyString())).thenReturn(false);
        assertThrows(BizException.class, () -> linkMobileAuthenticationOtpUseCase.re_generate(dto));
    }

    @Test
    @DisplayName("re_generate should return existing OTP when present")
    void reGenerate_shouldReturnExisting() {
        CreateOtpRequestDto dto = CreateOtpRequestDto.builder()
                .to("852-91234567")
                .usecase(RedisKey.OTP_CUSTOM)
                .build();
        String key = RedisKey.OTP_CUSTOM.getKey().concat("852-91234567");
        when(redisUtil.hasKey(OtpUtil.markAs(key, ":isOccupied"))).thenReturn(true);
        when(redisUtil.hasKey(key)).thenReturn(true);
        when(redisUtil.get(key)).thenReturn(OtpMetadata.builder().code("333333").counter(0).build());
        when(redisUtil.getExpire(key)).thenReturn(45L);

        OtpMetadata result = linkMobileAuthenticationOtpUseCase.re_generate(dto);
        assertEquals("333333", result.getCode());
        assertEquals(45, result.getTtl());
    }

    @Test
    @DisplayName("markAsOccupied should set redis key")
    void markAsOccupied_shouldSet() {
        linkMobileAuthenticationOtpUseCase.markAsOccupied("otp:custom:852-91234567");
        verify(redisUtil).set(contains(":isOccupied"), any(), eq(1800L));
    }

    @Test
    @DisplayName("successAndCleanupRedis should mark verified")
    void successAndCleanup_shouldMarkVerified() {
        String redisKey = "otp:custom:852-91234567";
        when(redisUtil.get(redisKey)).thenReturn(RegisterOtpMetadata.builder()
                .code("333333").counter(0).isVerified(false).build());

        linkMobileAuthenticationOtpUseCase.successAndCleanupRedis(redisKey);

        verify(redisUtil).set(eq(OtpUtil.markAsVerified(redisKey)), any(RegisterOtpMetadata.class), eq(1800L));
        verify(redisUtil).delete(redisKey);
    }

    @Test
    @DisplayName("validationRecordInRedis should build metadata")
    void validationRecord_shouldBuild() {
        RegisterOtpMetadata metadata = linkMobileAuthenticationOtpUseCase.validationRecordInRedis(
                CreateOtpRequestDto.builder().digit(6).type(OtpType.DIGIT).build());
        assertNotNull(metadata.getCode());
        assertFalse(metadata.getIsVerified());
    }

    @Test
    @DisplayName("triggerNotification should force template name")
    void triggerNotification_shouldForceTemplate() {
        BaseNotificationEvent event = mock(BaseNotificationEvent.class);
        linkMobileAuthenticationOtpUseCase.triggerNotification(event, "ignored");
        verify(event).setNotificationTemplateName("otp.user-register");
    }
}
