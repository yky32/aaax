package com.aaax.server.usecase;

import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.enu.KafkaTopic;
import com.aaax.core.kafka.event.UserStateMutatedEvent;
import com.aaax.core.utils.InstantUtil;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.config.redis.RedisKey;
import com.aaax.server.entity.dto.json_context.CredentialHistoryMetadata;
import com.aaax.server.entity.dto.json_context.OtpMetadata;
import com.aaax.server.entity.dto.json_context.SystemConfigMetadata;
import com.aaax.server.entity.dto.request.CreateOtpRequestDto;
import com.aaax.server.entity.dto.request.ForgotPasswordRequestDto;
import com.aaax.server.entity.dto.request.VerifyOtpRequestDto;
import com.aaax.server.entity.dto.response.GetSystemConfigurationRequestDto;
import com.aaax.server.entity.dto.response.PendingVerifyUserResponseDto;
import com.aaax.server.entity.enu.OtpSystemConfigTarget;
import com.aaax.server.entity.enu.OtpType;
import com.aaax.server.entity.po.configuration.SystemConfiguration;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.exception.response.UaaErrorResponse;
import com.aaax.server.repository.AuthenticationRepository;
import com.aaax.server.repository.UserRepository;
import com.aaax.server.service.AuthenticationService;
import com.aaax.server.service.DtoWrapper;
import com.aaax.server.service.UaaService;
import com.aaax.server.usecase.otp.ForgotPasswordOtpUseCase;
import com.aaax.server.utils.OtpUtil;
import com.aaax.server.validation.UaaValidation;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Forgot-password journey (login lock is delayed until OTP succeeds — anti lockout DoS):
 * <ol>
 *   <li>Initiate — send OTP only; do <strong>not</strong> deactivate auth (user can still login).</li>
 *   <li>Validate OTP — on success, set {@code authentication.is_active=false} until new password is set.</li>
 *   <li>Update password — replace credentials and set {@code is_active=true} again.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResetPasswordUseCase {

    private static final String KAFKA_EVENT = KafkaTopic.USER_STATE_CHANGED;
    private static final RedisKey REDIS_KEY = RedisKey.OTP_RESET_PASSWORD;
    private static final String SYSTEM_CONFIG_OTP_TARGET = OtpSystemConfigTarget.OTP_RESET_PASSWORD_TTL;
    private final RedisUtil redisUtil;
    private final KafkaUtil kafkaUtil;
    private final UaaService uaaService;
    private final AuthenticationService  authenticationService;
    private final UserRepository userRepository;
    private final AuthenticationRepository authenticationRepository;
    private final ForgotPasswordOtpUseCase forgotPasswordOtpUseCase;
    private final PasswordEncoder passwordEncoder;
    private final SystemConfigurationUseCase systemConfigurationUseCase;
    @Value("${aaax.config.credentials-history-size:0}")
    private Integer credentialsHistorySize;

    // === UTIL
    // === UTIL
    // === UTIL
    private static @NotNull String isVerified(String username) {
        return OtpUtil.markAsVerified(REDIS_KEY.getKey().concat(username));
    }

    // === STEP 1 — OTP only (no login lock)
    public PendingVerifyUserResponseDto initiate(@Valid ForgotPasswordRequestDto dto) {
        if (dto.getUsername() != null) {
            dto.setUsername(UaaValidation.toCanonicalIdentifier(dto.getUsername()));
        }
        // ====== check is verified == early return
        String verifiedKey = isVerified(dto.getUsername());
        if (redisUtil.hasKey(verifiedKey)) {
            forgotPasswordOtpUseCase.successAndCleanupRedis(verifiedKey);
        }
        // ===== hold up the username for use in 30-min
        forgotPasswordOtpUseCase.markAsOccupied(REDIS_KEY.getKey().concat(dto.getUsername()));

        // Ensure account exists; do NOT deactivate auth here (prevents lockout DoS via "forgot password").
        try {
            uaaService.getByUsername(dto.getUsername());
        } catch (Exception e) {
            // for user not found case — same generic response (no enumeration)
            return DtoWrapper.getDefaultPendingVerifyUserResponseDto(dto.getUsername());
        }

        OtpMetadata otpMetadata = forgotPasswordOtpUseCase.generate(this._forgotPasswordRequestDto(dto), "forgot-password");
        return DtoWrapper.getPendingVerifyUserResponseDto(dto.getUsername(), otpMetadata);
    }

    // === STEP 2 — correct OTP → lock login until new password is set
    public boolean validate(@Valid ForgotPasswordRequestDto requestDto) {
        prepareForgotDto(requestDto);
        VerifyOtpRequestDto dto = VerifyOtpRequestDto.builder()
                .to(requestDto.getUsername())
                .code(requestDto.getCode())
                .usecase(REDIS_KEY)
                .build();
        // throws on wrong OTP / expired; returns true only when verified
        boolean ok = forgotPasswordOtpUseCase.verify(dto);
        if (ok) {
            this._lockAuthenticationUntilPasswordReset(requestDto.getUsername());
        }
        return ok;
    }

    /**
     * After OTP succeeds, block password login until {@link #updateNewPassword} reactivates.
     */
    private void _lockAuthenticationUntilPasswordReset(String username) {
        try {
            Authentication authentication = uaaService.getByUsername(username);
            if (Boolean.FALSE.equals(authentication.getIsActive())) {
                return;
            }
            authentication.setIsActive(false);
            authenticationRepository.saveAndFlush(authentication);
            log.info("-- forgot-password OTP verified; locked auth login for username={}", username);
        } catch (Exception e) {
            // Should be rare (user deleted mid-flow); verified Redis key still gates password update.
            log.warn("-- forgot-password lock skipped for username={}: {}", username, e.getMessage());
        }
    }

    // === STEP 2.1
    public PendingVerifyUserResponseDto regenerateOtp(ForgotPasswordRequestDto requestDto) {
        prepareForgotDto(requestDto);
        OtpMetadata otpMetadata = forgotPasswordOtpUseCase.re_generate(this._forgotPasswordRequestDto(requestDto));
        return DtoWrapper.getPendingVerifyUserResponseDto(requestDto.getUsername(), otpMetadata);
    }

    // === STEP 3
    @Transactional
    public GetUserResponseDto updateNewPassword(ForgotPasswordRequestDto requestDto) {
        prepareForgotDto(requestDto);
        // === check all steps was passed. in redis Record as [1-Factor Authentication]
        String redisKey = isVerified(requestDto.getUsername());
        // ==== final validations
        if (!redisUtil.hasKey(redisKey)) {
            throw new BizException(UaaErrorResponse.UAA0400, "No Verified key. => ".concat(requestDto.getUsername()));
        }
        // ==== final validations end

        GetUserResponseDto execute = this.execute(requestDto);
        // ==== clean cache
        redisUtil.delete(redisKey);
        return execute; // [CONFIRM] can open a user with correct password.
    }

    private GetUserResponseDto execute(ForgotPasswordRequestDto dto) {
        return this.execute(dto, UserStatus.ACTIVE);
    }

    @SneakyThrows
    @Transactional
    public GetUserResponseDto execute(ForgotPasswordRequestDto dto, UserStatus status) {
        // __ validation, determine the [username] --> as Mobile/Email
        prepareForgotDto(dto);
        String username = dto.getUsername();
        log.info("=================== ForgotPasswordRequestDto.username => {}", username);

        // 2.0 Update the latest/updated Password.
        Authentication authentication = authenticationService.findByDynamicIdentifier(dto.getUsername());

        if (credentialsHistorySize > 0) {
            // ===2.0.1 do-handle old password and logic check existed.
            Instant now = Instant.now();
            List<CredentialHistoryMetadata> credentialHistories = Optional.ofNullable(authentication.getCredentialsHistories()).orElse(new ArrayList<>(10));
            credentialHistories.add(CredentialHistoryMetadata.builder()
                    .credentials(authentication.getCredentials()) // old password --> keep record
                    ._createDt(now)
                    .createDt(InstantUtil.parse(now))
                    .build());
            // ===2.0.2 Sort credentialHistories in descending order by createDt
            // Sort in descending order by createDt and keep only the latest 10 items
            List<CredentialHistoryMetadata> sortedHistories = credentialHistories.stream()
                    .sorted(Comparator.comparing(CredentialHistoryMetadata::getCreateDt).reversed())
                    .limit(10) // Keep only the top 10
                    .toList();

            // ===2.0.3 Start processing new password.
            // Check if any of the latest 10 entries contain the newPassword
            boolean isExisted = sortedHistories.stream().anyMatch(history -> history.getCredentials() != null && passwordEncoder.matches(dto.getCredentials(), history.getCredentials()));
            if (isExisted) {
                throw new BizException(UaaErrorResponse.UAA0003, "Existed in the previous list size. => ".concat(String.valueOf(sortedHistories.size())));
            }
            authentication.setCredentialsHistories(sortedHistories);
        }
        Optional<SystemConfiguration> config = systemConfigurationUseCase.getOptionalSystemConfig("USER_CREDENTIALS_REQUIREMENT_REG_EXP", "GLOBAL");
        List<String> regexps = new ArrayList<>();
        if (config.isPresent()) {
            regexps = JSONUtil.convertFromObject(config.get().getValue(), new TypeReference<>() {});
        }
        authentication.setCredentials(UaaValidation.check_passwordRequirement(passwordEncoder, dto.getCredentials(), regexps));
        authentication.setIsActive(true); // resumed this authentication
        authenticationRepository.saveAndFlush(authentication);

        // 2.1 RE-Open User Status
        authentication.getUser().setStatus(status);
        userRepository.saveAndFlush(authentication.getUser());

        // 3 __ Trigger user state changed event
        UserStateMutatedEvent event = UserStateMutatedEvent.builder()
                .userId(String.valueOf(authentication.getUser().getId()))
                .username(authentication.getUser().getUsername())
                .requestId(UUID.randomUUID().toString())
                .eventName(KAFKA_EVENT)
                .build();
        log.info("==================== ForgotPasswordRequestDto event updated : [{}]", event);
        kafkaUtil.send(KAFKA_EVENT, event);
        log.info("-- ForgotPasswordRequestDto user : {}", authentication.getUser().getUsername());
        Thread.sleep(1 * 1000); // delay the thread to client to prevent event being late-consumed.
        return DtoWrapper.getUserResponseDto(authentication.getUser(), authentication.getUser().getAuthentications());
    }

    private CreateOtpRequestDto _forgotPasswordRequestDto(ForgotPasswordRequestDto dto) {
        Optional<SystemConfiguration> config = systemConfigurationUseCase.getOptionalSystemConfig("OTP_RESET_PASSWORD_TEMPLATE", Optional.ofNullable(dto.getSourceSystem()).isPresent() ? dto.getSourceSystem() : "GLOBAL");
        return CreateOtpRequestDto.builder()
                .to(dto.getUsername())
                .usecase(REDIS_KEY)
                .type(OtpType.DIGIT)
                .digit(6)
                .systemConfig(SystemConfigMetadata.builder()
                        .target(SYSTEM_CONFIG_OTP_TARGET)
                        .scope(dto.getSourceSystem())
                        .build())
                .metadata(dto.getMetadata())
                .sourceSystem(dto.getSourceSystem())
                .templateId(config.map(systemConfiguration -> String.valueOf(systemConfiguration.getValue())).orElse(null))
                .build();
    }

    public void forgotPasswordValidation(ForgotPasswordRequestDto requestDto) {
        switch (requestDto.getSourceSystem()) {
            default -> {
            }
        }
    }

    private void prepareForgotDto(ForgotPasswordRequestDto dto) {
        if (dto == null || dto.getUsername() == null) {
            return;
        }
        dto.setUsername(UaaValidation.toCanonicalIdentifier(dto.getUsername()));
    }
}
