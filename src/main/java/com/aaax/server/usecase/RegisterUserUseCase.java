package com.aaax.server.usecase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.aaax.core.api.UtilApiClient;
import com.aaax.core.common.jsonfield.UserMetadata;
import com.aaax.core.constant.RegexPatternConstant;
import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.entity.dto.KeyValue;
import com.aaax.core.entity.dto.aaax.response.GetUserResponseDto;
import com.aaax.core.entity.dto.util.response.GetRefDataResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.enu.KafkaTopic;
import com.aaax.core.kafka.event.UserAliasGeneratedEvent;
import com.aaax.core.kafka.event.UserCreatedEvent;
import com.aaax.core.kafka.event.UserDeletedEvent;
import com.aaax.core.response.SystemResponse;
import com.aaax.core.utils.*;
import com.aaax.server.config.redis.RedisKey;
import com.aaax.server.entity.dto.json_context.OtpMetadata;
import com.aaax.server.entity.dto.request.CreateOtpRequestDto;
import com.aaax.server.entity.dto.request.RegisterUserRequestDto;
import com.aaax.server.entity.dto.request.UpdatePasswordRequestDto;
import com.aaax.server.entity.dto.request.VerifyOtpRequestDto;
import com.aaax.server.entity.dto.response.GetSystemConfigurationRequestDto;
import com.aaax.server.entity.dto.response.PendingVerifyUserResponseDto;
import com.aaax.server.entity.enu.OtpType;
import com.aaax.server.entity.po.configuration.SystemConfiguration;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.exception.response.OtpErrorResponse;
import com.aaax.server.exception.response.SystemConfigurationErrorResponse;
import com.aaax.server.exception.response.AaaxErrorResponse;
import com.aaax.server.exception.response.UseRegistrationErrorResponse;
import com.aaax.server.repository.AuthenticationRepository;
import com.aaax.server.repository.UserRepository;
import com.aaax.server.service.AuthenticationService;
import com.aaax.server.service.DtoWrapper;
import com.aaax.server.service.AaaxService;
import com.aaax.server.usecase.otp.RegisterUserOtpUseCase;
import com.aaax.server.utils.OtpUtil;
import com.aaax.server.validation.PasswordPolicy;
import com.aaax.server.validation.AaaxValidation;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

import static com.aaax.server.validation.AaaxValidation.detechLoginType;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegisterUserUseCase {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationRepository authenticationRepository;
    private final KafkaUtil kafkaUtil;
    private final RedisUtil redisUtil;
    private final AaaxService aaaxService;
    private final AuthenticationService authenticationService;
    private final RegisterUserOtpUseCase registerUserOtpUseCase;
    private final SystemConfigurationUseCase systemConfigurationUseCase;
    private final UserProfileUseCase userProfileUseCase;
    private final UserPreferenceUseCase userPreferenceUseCase;
    private final UtilApiClient utilApiClient;
    private final PasswordPolicy passwordPolicy;
    @Value("${aaax.config.user-created-waiting-time-ms:1}")
    private long userCreatedWaitingTimeMs;
    @Value("${aaax.config.system-invoker}")
    private String systemInvoker;
    @Value("${ext.util-enabled:false}")
    private boolean utilEnabled;

    private static @NotNull String isVerified(String username) {
        return OtpUtil.markAsVerified(RedisKey.OTP_USER_REGISTER.getKey().concat(username));
    }

    public boolean verify(RegisterUserRequestDto requestDto) {
        prepareRegisterDto(requestDto);
        VerifyOtpRequestDto dto = VerifyOtpRequestDto.builder()
                .to(requestDto.getUsername())
                .code(requestDto.getCode())
                .usecase(RedisKey.OTP_USER_REGISTER)
                .build();
        return registerUserOtpUseCase.verify(dto);
    }

    public PendingVerifyUserResponseDto regenerateRegisterOtp(RegisterUserRequestDto requestDto) {
        prepareRegisterDto(requestDto);
        OtpMetadata otpMetadata = registerUserOtpUseCase.re_generate(this._configuredAndFetchedFromDb(requestDto));
        return DtoWrapper.getPendingVerifyUserResponseDto(requestDto.getUsername(), otpMetadata);
    }

    /**
     * @param requestDto - only [username] and [credentials] are needed
     * @return - GetUserResponseDto
     */
    public GetUserResponseDto execute_external(RegisterUserRequestDto requestDto) {
        prepareRegisterDto(requestDto);
        // ===========  VALIDATIONS
        if (StringUtils.isBlank(requestDto.getUsername())) {
            throw new BizException(AaaxErrorResponse.AAAX0400, "[username] was empty/null of ".concat(this.getClass().getSimpleName()));
        }
        if (StringUtils.isBlank(requestDto.getCredentials())) {
            throw new BizException(AaaxErrorResponse.AAAX0400, "[credentials] was empty/nul of ".concat(this.getClass().getSimpleName()));
        }
        // ===========  VALIDATIONS

        // === check all steps was passed. in redis Record as [1-Factor Authentication]
        String redisKey = isVerified(requestDto.getUsername());
        // ==== final validations
        if (!redisUtil.hasKey(redisKey)) {
            throw new BizException(AaaxErrorResponse.AAAX0400, "No Verified key. => ".concat(requestDto.getUsername()));
        }
        // ==== final validations end
        GetUserResponseDto execute = this.execute(requestDto);

        // ==== clean cache
        redisUtil.delete(redisKey);
        return execute; // [CONFIRM] can open a user with correct password.
    }

    @SneakyThrows
    public PendingVerifyUserResponseDto register_public(RegisterUserRequestDto requestDto) {
        prepareRegisterDto(requestDto);
        // ====== check is verified == early return
        if (redisUtil.hasKey(isVerified(requestDto.getUsername()))) {
            throw new BizException(OtpErrorResponse.OTP2001, requestDto.getUsername());
        }
        // ====== check accounts existed (case-insensitive for email)
        this.checkUsernameExisted(requestDto.getUsername());

        // ===== hold up the username for use in 30-min
        registerUserOtpUseCase.markAsOccupied(RedisKey.OTP_USER_REGISTER.getKey().concat(requestDto.getUsername()));
        // ===== hold up the username for use in 30-min

        OtpMetadata otpMetadata = registerUserOtpUseCase.generate(
                this._configuredAndFetchedFromDb(requestDto),
                "public-user-register"
        );
        Thread.sleep(500);
        return DtoWrapper.getPendingVerifyUserResponseDto(requestDto.getUsername(), otpMetadata);
    }

    /**
     * Availability probe only ({@code POST …/registrations?check=1}).
     * Occupied → same 409 as full register; free → 200 with username, <b>no</b> OTP / hold.
     */
    public PendingVerifyUserResponseDto register_public_checkOnly(RegisterUserRequestDto requestDto) {
        prepareRegisterDto(requestDto);
        this.checkUsernameExisted(requestDto.getUsername());
        return DtoWrapper.getDefaultPendingVerifyUserResponseDto(requestDto.getUsername());
    }

    /** Email/username → lowercase for identity; mutates dto.username in place. */
    private void prepareRegisterDto(RegisterUserRequestDto requestDto) {
        if (requestDto == null || StringUtils.isBlank(requestDto.getUsername())) {
            return;
        }
        requestDto.setUsername(AaaxValidation.toCanonicalIdentifier(requestDto.getUsername()));
    }

    private CreateOtpRequestDto _configuredAndFetchedFromDb(RegisterUserRequestDto dto) {
        Optional<SystemConfiguration> config = systemConfigurationUseCase.getOptionalSystemConfig("OTP_REGISTER_TEMPLATE", Optional.ofNullable(dto.getSourceSystem()).isPresent() ? dto.getSourceSystem() : "GLOBAL");
        switch (dto.getSourceSystem()) {
            default -> {
                return CreateOtpRequestDto.builder()
                        .to(dto.getUsername())
                        .usecase(RedisKey.OTP_USER_REGISTER)
                        .type(OtpType.DIGIT)
                        .digit(6)
                        .metadata(dto.getMetadata())
                        .sourceSystem(dto.getSourceSystem())
                        .templateId(config.map(systemConfiguration -> String.valueOf(systemConfiguration.getValue())).orElse(null))
                        .build();
            }
        }
    }

    private void checkUsernameExisted(String username) {
        authenticationService.isThisUsernameExistedForPublicRegister(username);
    }

    public GetUserResponseDto execute(RegisterUserRequestDto dto) {
        UserMetadata metadata = UserMetadata.builder()
                .build();
        GetUserResponseDto user = this.execute(dto, UserStatus.ACTIVE, metadata);
        try {
            this.post_action_of_feature(dto, user.getId(), user.getUsername());
        } catch (Exception exception) {
            UserDeletedEvent event = UserDeletedEvent.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .build();
            kafkaUtil.send(KafkaTopic.USER_DELETED, event);
            throw new BizException(UseRegistrationErrorResponse.URG0001, Map.of("username", user.getUsername()));
        }
        return user;
    }

    @SneakyThrows
    @Transactional
    public GetUserResponseDto execute(RegisterUserRequestDto dto, UserStatus status, UserMetadata metadata) {
        // __ validation, determine the [username] --> as Mobile/Email
        prepareRegisterDto(dto);
        String username = dto.getUsername();
        log.info("=================== RegisterUserUseCase.username => {}", username);
        LoginType loginType = detechLoginType(username);

        // __ validation, [Authentication] is sufficient to check identifier is used xor not
        User user;
        Optional<Authentication> authenticationOptional = authenticationService.findOptionalByDynamicIdentifier(username);
        if (authenticationOptional.isPresent()) {
            // == existed user.
            // == check status one-by-one
            user = aaaxService.getById(authenticationOptional.get().getUser().getId());
            switch (user.getStatus()) {
                case ACTIVE -> {
                    log.info("-- user existed {} and return", username);
                    throw new BizException(AaaxErrorResponse.AAAX0409, "username =>".concat(username));
                }
                case PENDING_VERIFY -> user.setStatus(status); // active the users
                case INACTIVE -> throw new BizException(AaaxErrorResponse.AAAX0004, "username =>".concat(username));
            }
        } else {
            // == [branch-new] user. Always store canonical lowercase email/username.
            user = User.builder()
                    .username(username)
                    .status(status)
                    .metadata(metadata)
                    .build();
            Authentication authentication = Authentication.builder()
                    .credentials(passwordPolicy.encode(passwordEncoder, dto.getCredentials()))
                    .identifier(username)
                    .user(user)
                    .loginType(loginType)
                    .build();
            user.setAuthentications(Collections.singletonList(authentication));
        }
        // default ss is systemInvoker
        user.setSourceSystemTags(Optional.ofNullable(dto.getSourceSystem()).isEmpty() ? List.of(systemInvoker) : List.of(dto.getSourceSystem()));
        user = userRepository.saveAndFlush(user);

        // AUTO - CREATED [user - settings]
        userProfileUseCase.doCreateDefault(dto.getMetadata(), user.getId());
        userPreferenceUseCase.doCreateDefault(String.valueOf(user.getId()), null);
        // AUTO - CREATED [user - settings]

        // __ Trigger user created event
        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .requestId(UUID.randomUUID().toString())
                .sourceSystems(Optional.ofNullable(dto.getSourceSystem()).isPresent() ? List.of(dto.getSourceSystem()) : List.of())
                .eventName(KafkaTopic.USER_CREATED)
                .build();
        log.info("==================== RegisterUserUseCase event created : [{}]", event);
        kafkaUtil.send(KafkaTopic.USER_CREATED, event);
        log.info("-- RegisterUserUseCase user : {}", user.getUsername());
        Thread.sleep(userCreatedWaitingTimeMs);
        GetUserResponseDto userResponseDto = DtoWrapper.getUserResponseDto(user, user.getAuthentications());
        if (!dto.getExtraFeatures().isEmpty()) userResponseDto.setStatus(null);
        userResponseDto.setMetadata(null); // remove when register response.
        return userResponseDto;
    }


    private void post_action_of_feature(RegisterUserRequestDto dto, String userId, String username) {
    }

    private String getNextAlias(String entityType) {
        Long nextAlias = userRepository.getNextAlias();
        GetSystemConfigurationRequestDto idPrefix1 = systemConfigurationUseCase.myConfigurations("USER_ALIAS_PREFIX_1");
        GetSystemConfigurationRequestDto idPrefix2 = systemConfigurationUseCase.myConfigurations("USER_ALIAS_PREFIX_2");
        GetSystemConfigurationRequestDto idSuffix0 = systemConfigurationUseCase.myConfigurations("USER_ALIAS_SUFFIX_0");
        List<KeyValue> keyValues = JSONUtil.convertFromObject(idPrefix2.getValue(), new TypeReference<>() {
        });
        KeyValue keyValue = keyValues.stream()
                .filter(entry -> entry.getKey().equals(entityType))
                .findFirst()
                .orElseThrow(() -> new BizException(SystemConfigurationErrorResponse.SYC0001, Map.of("key", "ID_PREFIX_2", "config", keyValues, "checkingKey", entityType)));
        return String.valueOf(idPrefix1.getValue()).concat(String.valueOf(keyValue.getValue())).concat(String.format(String.valueOf(idSuffix0.getValue()), nextAlias));
    }

    public GetUserResponseDto updateCredentials(UpdatePasswordRequestDto dto, String identifier) {
        log.info("-- RegisterUserUseCase updateCredentials start : {} - {}", identifier, dto);
        Authentication auth = aaaxService.getByUsername(identifier);
        if (dto.getCredentials() != null) {
            // check existing password
            if (!passwordEncoder.matches(dto.getExistingCredentials(), auth.getCredentials())) {
                log.info("-- RegisterUserUseCase updateCredentials : {} -> Incorrect existing password", identifier);
                throw new BizException(AaaxErrorResponse.AAAX0003, "Incorrect existing password.");
            }
            if (passwordEncoder.matches(dto.getCredentials(), auth.getCredentials())) {
                log.info("-- RegisterUserUseCase updateCredentials : {} -> {}", identifier, AaaxErrorResponse.AAAX0003.getMessage());
                throw new BizException(AaaxErrorResponse.AAAX0003);
            }
            auth.setCredentials(passwordPolicy.encode(passwordEncoder, dto.getCredentials()));
            authenticationRepository.save(auth);
            log.info("-- RegisterUserUseCase updateCredentials end : {} - {}", identifier, dto);
        }
        return DtoWrapper.getUserResponseDto(auth.getUser(), List.of(auth));
    }
    /**
     * @param username - email or mobile phone. from client-side
     * @return - alias
     */
    private String detectAlias(String username) {
        if (username == null) {
            throw new BizException(SystemResponse.PAM0400, "[username] was null of detectAlias");
        }
        username = username.trim();
        if (username.contains("@")) {
            return username.split("@", 2)[0];
        }
        return username;
    }

    public User executeFrom3rdParty(String username, LoginType loginType, UserMetadata metadata) {
        return executeFrom3rdParty(username, username, loginType, metadata);
    }

    public User executeFrom3rdParty(String username, String authIdentifier, LoginType loginType, UserMetadata metadata) {
        String _username = username.toLowerCase();
        User user = User.builder()
                .username(_username)
                .status(UserStatus.ACTIVE)
                .metadata(metadata)
                .sourceSystemTags(List.of(systemInvoker))
                .build();
        Authentication thirdPartyAuthentication = Authentication.builder()
                .credentials("-")
                .identifier(authIdentifier)
                .user(user)
                .loginType(loginType)
                .build();

        List<Authentication> authentications = new ArrayList<>();
        authentications.add(thirdPartyAuthentication);
        user.setAuthentications(authentications);
        return userRepository.saveAndFlush(user);
    }

    public void registerValidations(RegisterUserRequestDto requestDto) {
        // ==== VALIDATIONS (local only — util ref-data optional)
        if (Optional.ofNullable(requestDto.getMetadata()).isPresent()) {
            String _phone = (String) requestDto.getMetadata().get("phone");
            String _areaCode = (String) requestDto.getMetadata().get("areaCode");

            if (Optional.ofNullable(_phone).isPresent() || Optional.ofNullable(_areaCode).isPresent()) {
                if (Optional.ofNullable(_areaCode).isEmpty()) {
                    throw new BizException(SystemResponse.PAM0400, "Plz provide [%s]".formatted("areaCode"));
                }
                if (utilEnabled) {
                    try {
                        GetRefDataResponseDto areaCode =
                                RetrofitCallHandler.execute(utilApiClient.getRefDataByKey("common.area-code"));
                        ((List<Map>) areaCode.getValue())
                                .stream()
                                .filter(map -> map.get("code").equals(_areaCode))
                                .findFirst()
                                .orElseThrow(() -> new BizException(
                                        AaaxErrorResponse.AAAX4400,
                                        "[%s] => [%s]".formatted("areaCode", _areaCode)));
                    } catch (BizException e) {
                        throw e;
                    } catch (Exception e) {
                        log.warn("util area-code check skipped: {}", e.getMessage());
                    }
                }
                ValidationUtil.patternMatches(_phone, RegexPatternConstant.IS_DIGIT);
            }
        }
    }
}
