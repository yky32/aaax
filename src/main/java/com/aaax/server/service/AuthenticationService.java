package com.aaax.server.service;


import com.nimbusds.jose.jwk.RSAKey;
import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.event.LoginAttemptsMutatedEvent;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.config.aop.log.ActivityLog;
import com.aaax.server.entity.dto.json_context.OtpMetadata;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.exception.response.AaaxErrorResponse;
import com.aaax.server.repository.AuthenticationRepository;
import com.aaax.server.repository.UserRepository;
import com.aaax.server.usecase.otp.ForgotPasswordOtpUseCase;
import com.aaax.server.utils.CryptographyUtil;
import com.aaax.server.validation.AaaxValidation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.aaax.core.kafka.enu.KafkaTopic.USER_LOGIN_ATTEMPTS_MUTATED;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final AuthenticationRepository authenticationRepository;
    private final PasswordEncoder encoder;
    private final ForgotPasswordOtpUseCase forgotPasswordOtpUseCase;
    private final KafkaUtil kafkaUtil;
    private final RedisUtil redisUtil;
    private final RSAKey rsaKey;
    @PersistenceContext
    private EntityManager entityManager;

    public Boolean authenticate(String storedCredentials, String credentials) {
        return encoder.matches(credentials, storedCredentials);
    }

    /**
     * Finds Optional authentication by identifier (email/phone/username etc.)
     * Identifier is canonicalized (email/username case-insensitive).
     */
    public Optional<Authentication> findOptionalByDynamicIdentifier(String identifier) {
        String canonical = AaaxValidation.toCanonicalIdentifier(identifier);
        LoginType loginType = AaaxValidation.detechLoginType(canonical);
        return authenticationRepository.findByIdentifierIgnoreCaseAndLoginType(canonical, loginType);
    }

    /**
     * Finds authentication by identifier (email/phone/username etc.)
     */
    public Authentication findByDynamicIdentifier(String identifier) {
        return findOptionalByDynamicIdentifier(identifier)
                .orElseThrow(() -> new BizException(AaaxErrorResponse.AAAX0001,
                        "User not found with identifier: " + identifier));
    }

    /**
     * Same as above but throws exception if not found or inactive (soft deleted)
     */
    public Authentication findValidRecordsByDynamicIdentifier(String identifier) {
        return findOptionalByDynamicIdentifier(identifier)
                .filter(Authentication::getIsActive)
                .orElseThrow(() -> new BizException(AaaxErrorResponse.AAAX0001,
                        "User not found with identifier: " + identifier));
    }

    /**
     * Explicit version when you already know the login type
     */
    public Authentication findByIdentifierWithLoginType(String identifier, LoginType loginType) {
        final String canonical = AaaxValidation.toCanonicalIdentifierIfPresent(identifier) != null
                ? AaaxValidation.toCanonicalIdentifierIfPresent(identifier)
                : identifier;
        return authenticationRepository.findByIdentifierIgnoreCaseAndLoginType(canonical, loginType)
                .filter(Authentication::getIsActive)
                .orElseThrow(() -> new BizException(AaaxErrorResponse.AAAX0001,
                        "User not found with identifier: " + identifier + ", loginType: " + loginType));
    }

    @Value("${aaax.security.max-login-attempts:5}")
    private int maxLoginAttempts = 5;

    // ============= series of checking for granting token to [user].
    // === 1. check username password
    // === 2. check over [session id] vs [user id]
    @ActivityLog
    public boolean check(Authentication authentication, String credentials) {
        return check_password(authentication, credentials)
                && check_user_status(authentication)
                && check_attempts(authentication.getAttempts())
                && check_device_binding()
                ;
    }

    /**
     * Login does not bind devices. {@code aaax.config.device-binding.mode} only
     * affects {@code POST /user-devices} (OFF vs TRUST_LATEST).
     */
    private boolean check_device_binding() {
        return true;
    }

    private boolean check_attempts(Integer attempts) {
        int n = Optional.ofNullable(attempts).orElse(0);
        log.info("-- authentication.attempts => [{}] max=[{}]", n, maxLoginAttempts);
        return n < maxLoginAttempts;
    }

    public Boolean check_password(Authentication authentication, String credentials) {
        Boolean isCorrectPassword = authenticate(authentication.getCredentials(), credentials);
        if (!isCorrectPassword) {
            log.info("-- authenticationService.check check_password: {} - {}", authentication.getIdentifier(), "Incorrect Password.");
        }
        return isCorrectPassword;
    }

    private boolean check_user_status(Authentication authentication) {
        User user = userRepository.findById(authentication.getUser().getId())
                .orElseThrow(() -> new BizException(AaaxErrorResponse.AAAX0001,
                        "User not found with id: " + authentication.getUser().getId()));
        log.info("-- authenticationService.check check_userStatus: {} - {}", authentication.getIdentifier(), user.getStatus().name());
        return Objects.requireNonNull(user.getStatus()) == UserStatus.ACTIVE
                && user.getIsActive() // SOFT_DELETE CASE
                ;
    }

    public void post_check(Authentication authentication, boolean isSuccess) {
        int current = Optional.ofNullable(authentication.getAttempts()).orElse(0);
        if (isSuccess) {
            authentication.setAttempts(0);
            authentication.setLastLoginDt(java.time.Instant.now());
        } else {
            authentication.setAttempts(current + 1);
        }
        authenticationRepository.save(authentication);
        LoginAttemptsMutatedEvent event = LoginAttemptsMutatedEvent.builder()
                .userId(String.valueOf(authentication.getUser().getId()))
                .username(authentication.getIdentifier())
                .isSuccess(isSuccess)
                .build();
        kafkaUtil.send(USER_LOGIN_ATTEMPTS_MUTATED, event);
    }

    @SneakyThrows
    public String decrypt(String credentials) {
        return CryptographyUtil.decrypt(credentials.trim(), rsaKey.toPrivateKey());
    }

    /**
     * 409 -> user existed -> jump login()
     * 200 -> go to login
     *
     * @param username - username (any casing for email; matched case-insensitively; stored/looked up as canonical)
     */
    public void isThisUsernameExistedForPublicRegister(String username) {
        String canonical = AaaxValidation.toCanonicalIdentifier(username);
        // __ validation, [Authentication] is sufficient to check identifier is used or not
        List<Authentication> authentications = authenticationRepository.findAllByIdentifierIgnoreCase(canonical);
        if (!authentications.isEmpty()) {
            authentications
                    .stream()
                    .filter(authentication -> this._isThisAuthenticationInvalid(authentication, canonical))
                    .findFirst()
                    .ifPresent(authentication -> {
                        boolean isVerifiedAlready = forgotPasswordOtpUseCase.isVerifiedAlready(canonical);
                        if (isVerifiedAlready) {
                            throw new BizException(AaaxErrorResponse.AAAX8420, "username =>".concat(canonical));
                        }
                        OtpMetadata otpMetadata = forgotPasswordOtpUseCase.queryBackTheStoredValueInRedis(canonical);
                        otpMetadata.setTo(canonical);
                        throw new BizException(AaaxErrorResponse.AAAX8400, otpMetadata);
                    });

            User user = authentications.stream().findFirst().get().getUser();
            switch (user.getStatus()) {
                case ACTIVE -> {
                    log.info("-- user existed {} (canonical={}) and return", username, canonical);
                    throw new BizException(AaaxErrorResponse.AAAX0409, "username =>".concat(canonical));
                }
            }
        }
    }

    private boolean _isThisAuthenticationInvalid(Authentication authentication, String username) {
        LoginType loginType = AaaxValidation.detechLoginType(username);
        // forgot password case -> loginType is same but authentication is invalid
        return loginType == authentication.getLoginType() && !authentication.getIsActive();
    }
}
