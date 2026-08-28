package com.aaax.server.usecase;

import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.entity.dto.aaax.response.GetUserResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.config.redis.RedisKey;
import com.aaax.server.entity.dto.json_context.OtpMetadata;
import com.aaax.server.entity.dto.request.ForgotPasswordRequestDto;
import com.aaax.server.entity.dto.response.PendingVerifyUserResponseDto;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.repository.AuthenticationRepository;
import com.aaax.server.repository.UserRepository;
import com.aaax.server.service.AuthenticationService;
import com.aaax.server.service.AaaxService;
import com.aaax.server.exception.response.OtpErrorResponse;
import com.aaax.server.usecase.otp.ForgotPasswordOtpUseCase;
import com.aaax.server.utils.OtpUtil;
import com.aaax.server.validation.PasswordPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResetPasswordUseCaseTest {

    @Mock
    private RedisUtil redisUtil;
    @Mock
    private KafkaUtil kafkaUtil;
    @Mock
    private AaaxService aaaxService;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationRepository authenticationRepository;
    @Mock
    private ForgotPasswordOtpUseCase forgotPasswordOtpUseCase;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordPolicy passwordPolicy;
    @Mock
    private SystemConfigurationUseCase systemConfigurationUseCase;

    @InjectMocks
    private ResetPasswordUseCase resetPasswordUseCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resetPasswordUseCase, "credentialsHistorySize", 0);
    }

    @Test
    @DisplayName("initiate should send OTP without deactivating auth (no lockout DoS)")
    void initiate_shouldSendOtpWithoutDeactivatingAuth() {
        User user = User.builder().id(1L).username("user@test.com").status(UserStatus.ACTIVE).build();
        user.setAuthentications(new ArrayList<>());
        Authentication auth = Authentication.builder().identifier("user@test.com").user(user).build();
        auth.setIsActive(true);
        when(redisUtil.hasKey(anyString())).thenReturn(false);
        when(aaaxService.getByUsername("user@test.com")).thenReturn(auth);
        when(systemConfigurationUseCase.getOptionalSystemConfig(anyString(), anyString())).thenReturn(Optional.empty());
        when(forgotPasswordOtpUseCase.generate(any(), eq("forgot-password")))
                .thenReturn(OtpMetadata.builder().code("123456").ttl(300).build());

        PendingVerifyUserResponseDto result = resetPasswordUseCase.initiate(
                ForgotPasswordRequestDto.builder().username("user@test.com").sourceSystem("APP").build());

        assertEquals("123456", result.getCode());
        verify(authenticationRepository, never()).saveAndFlush(any());
        assertTrue(auth.getIsActive());
        verify(forgotPasswordOtpUseCase).markAsOccupied(contains("user@test.com"));
    }

    @Test
    @DisplayName("initiate should return default dto when user not found")
    void initiate_shouldReturnDefaultWhenUserMissing() {
        when(redisUtil.hasKey(anyString())).thenReturn(false);
        when(aaaxService.getByUsername("missing@test.com")).thenThrow(new RuntimeException("not found"));

        PendingVerifyUserResponseDto result = resetPasswordUseCase.initiate(
                ForgotPasswordRequestDto.builder().username("missing@test.com").build());

        assertEquals("missing@test.com", result.getUsername());
        assertNull(result.getCode());
        verify(forgotPasswordOtpUseCase, never()).generate(any(), anyString());
    }

    @Test
    @DisplayName("initiate should cleanup existing verified key")
    void initiate_shouldCleanupVerifiedKey() {
        String verifiedKey = OtpUtil.markAsVerified(RedisKey.OTP_RESET_PASSWORD.getKey().concat("user@test.com"));
        when(redisUtil.hasKey(verifiedKey)).thenReturn(true);
        when(aaaxService.getByUsername("user@test.com")).thenThrow(new RuntimeException("skip"));

        resetPasswordUseCase.initiate(ForgotPasswordRequestDto.builder().username("user@test.com").build());

        verify(forgotPasswordOtpUseCase).successAndCleanupRedis(verifiedKey);
    }

    @Test
    @DisplayName("validate should lock auth after successful OTP")
    void validate_shouldLockAuthAfterSuccessfulOtp() {
        User user = User.builder().id(1L).username("user@test.com").status(UserStatus.ACTIVE).build();
        user.setAuthentications(new ArrayList<>());
        Authentication auth = Authentication.builder().identifier("user@test.com").user(user).build();
        auth.setIsActive(true);
        when(forgotPasswordOtpUseCase.verify(any())).thenReturn(true);
        when(aaaxService.getByUsername("user@test.com")).thenReturn(auth);

        assertTrue(resetPasswordUseCase.validate(
                ForgotPasswordRequestDto.builder().username("user@test.com").code("123456").build()));

        assertFalse(auth.getIsActive());
        verify(authenticationRepository).saveAndFlush(auth);
    }

    @Test
    @DisplayName("validate should not lock when OTP verify fails with exception")
    void validate_shouldNotLockWhenOtpThrows() {
        when(forgotPasswordOtpUseCase.verify(any())).thenThrow(new BizException(OtpErrorResponse.OTP0002));

        assertThrows(BizException.class, () -> resetPasswordUseCase.validate(
                ForgotPasswordRequestDto.builder().username("user@test.com").code("000000").build()));

        verify(authenticationRepository, never()).saveAndFlush(any());
        verify(aaaxService, never()).getByUsername(anyString());
    }

    @Test
    @DisplayName("regenerateOtp should wrap OTP metadata")
    void regenerateOtp_shouldWrap() {
        when(systemConfigurationUseCase.getOptionalSystemConfig(anyString(), anyString())).thenReturn(Optional.empty());
        when(forgotPasswordOtpUseCase.re_generate(any()))
                .thenReturn(OtpMetadata.builder().code("999999").ttl(60).build());

        PendingVerifyUserResponseDto result = resetPasswordUseCase.regenerateOtp(
                ForgotPasswordRequestDto.builder().username("user@test.com").sourceSystem("APP").build());

        assertEquals("999999", result.getCode());
    }

    @Test
    @DisplayName("updateNewPassword should throw when verified key missing")
    void updateNewPassword_shouldThrowWhenUnverified() {
        when(redisUtil.hasKey(anyString())).thenReturn(false);
        assertThrows(BizException.class, () -> resetPasswordUseCase.updateNewPassword(
                ForgotPasswordRequestDto.builder().username("user@test.com").credentials("NewPass1").build()));
    }

    @Test
    @DisplayName("updateNewPassword should update credentials and reactivate")
    void updateNewPassword_shouldUpdateCredentials() {
        String verifiedKey = OtpUtil.markAsVerified(RedisKey.OTP_RESET_PASSWORD.getKey().concat("user@test.com"));
        when(redisUtil.hasKey(verifiedKey)).thenReturn(true);
        User user = User.builder().id(1L).username("user@test.com").status(UserStatus.INACTIVE).build();
        user.setAuthentications(List.of());
        Authentication auth = Authentication.builder()
                .identifier("user@test.com")
                .credentials("old")
                .user(user)
                .build();
        when(authenticationService.findByDynamicIdentifier("user@test.com")).thenReturn(auth);
        when(passwordPolicy.encode(passwordEncoder, "NewPass1")).thenReturn("encoded-new");

        GetUserResponseDto result = resetPasswordUseCase.updateNewPassword(
                ForgotPasswordRequestDto.builder().username("user@test.com").credentials("NewPass1").build());

        assertEquals("u_1", result.getId());
        assertEquals("encoded-new", auth.getCredentials());
        assertTrue(auth.getIsActive());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(redisUtil).delete(verifiedKey);
        verify(kafkaUtil).send(anyString(), any());
    }

    @Test
    @DisplayName("forgotPasswordValidation should no-op for default source system")
    void forgotPasswordValidation_shouldNoOp() {
        assertDoesNotThrow(() -> resetPasswordUseCase.forgotPasswordValidation(
                ForgotPasswordRequestDto.builder().sourceSystem("APP").build()));
    }
}
