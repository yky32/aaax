package com.aaax.server.usecase;

import com.aaax.core.constant.RegexPatternConstant;
import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.enu.KafkaTopic;
import com.aaax.core.kafka.event.UserStateMutatedEvent;
import com.aaax.core.response.PaginationDto;
import com.aaax.core.response.SystemResponse;
import com.aaax.core.security.AuditAwareUser;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.JwtUtil;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.ValidationUtil;
import com.aaax.server.entity.dto.request.UpdatePasswordRequestDto;
import com.aaax.server.entity.dto.request.UpdateUsernameRequestDto;
import com.aaax.server.entity.dto.request.UpdateUserStatusRequestDto;
import com.aaax.server.entity.enu.UserProfileType;
import com.aaax.server.entity.po.UserRoute;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.entity.po.user_management.UserProfile;
import com.aaax.server.repository.*;
import com.aaax.server.service.AuthenticationService;
import com.aaax.server.service.DtoWrapper;
import com.aaax.server.service.UaaService;
import com.aaax.server.validation.UaaValidation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@AllArgsConstructor
@Slf4j
public class UserManagementUseCase {
    private final UserPermissionRepository userPermissionRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserTokenRepository userTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationRepository authenticationRepository;
    private final UserRouteRepository userRouteRepository;
    private final UserRepository userRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final UaaService uaaService;
    private final AuthenticationService authenticationService;
    private final KafkaUtil kafkaUtil;
    @PersistenceContext
    private EntityManager entityManager;

    public GetUserResponseDto updateCredentials(UpdatePasswordRequestDto dto, String identifier) {
        Authentication auth = uaaService.getByUsername(identifier);
        if (dto.getCredentials() == null || StringUtils.isEmpty(dto.getCredentials())) {
            throw new BizException(SystemResponse.PAM0400, "Please provide password value.");
        }
        auth.setCredentials(UaaValidation.check_passwordRequirement(passwordEncoder, dto.getCredentials(), List.of()));
        authenticationRepository.save(auth);
        return DtoWrapper.getUserResponseDto(auth.getUser(), List.of(auth));
    }

    /**
     * Admin rename of login identifier: updates {@code user.username}, matching
     * {@code authentication.identifier} (+ login type), and aligns DEFAULT
     * {@code UserProfile} ({@code context.email}, alias when still the old login).
     * Used by portal account management.
     */
    @Transactional
    public GetUserResponseDto updateUsername(UpdateUsernameRequestDto dto, String identifier) {
        if (dto == null || StringUtils.isBlank(dto.getUsername())) {
            throw new BizException(SystemResponse.PAM0400, "Please provide username value.");
        }
        String current = identifier == null ? "" : identifier.trim().toLowerCase();
        String next = dto.getUsername().trim().toLowerCase();
        if (StringUtils.isBlank(current)) {
            throw new BizException(SystemResponse.PAM0400, "identifier is required.");
        }
        Authentication auth = uaaService.getByUsername(current);
        User user = auth.getUser();
        if (next.equals(current) || next.equalsIgnoreCase(StringUtils.defaultString(user.getUsername()))) {
            return DtoWrapper.getUserResponseDto(user, List.of(auth));
        }
        // Throws when another active identity already owns this username
        authenticationService.isThisUsernameExistedForPublicRegister(next);
        LoginType loginType = UaaValidation.detechLoginType(next);
        user.setUsername(next);
        auth.setIdentifier(next);
        auth.setLoginType(loginType);
        userRepository.saveAndFlush(user);
        authenticationRepository.saveAndFlush(auth);
        this._alignUserProfileOnUsernameRename(user.getId(), current, next);
        log.info("-- updateUsername {} -> {}", current, next);
        return DtoWrapper.getUserResponseDto(user, List.of(auth));
    }

    /**
     * Keep DEFAULT UserProfile aligned with login rename — same conventions as
     * {@link UserProfileUseCase#doCreateDefault}: {@code context.email} from username
     * shape; {@code alias} only when it still matched the previous login identifier.
     */
    private void _alignUserProfileOnUsernameRename(Long userId, String current, String next) {
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserIdAndType(userId, UserProfileType.DEFAULT.name());
        if (profileOpt.isEmpty()) {
            log.warn("-- updateUsername: no UserProfile for userId={}, skipping profile sync", userId);
            return;
        }
        UserProfile profile = profileOpt.get();
        Map context = Optional.ofNullable(JSONUtil.convertFromObject(profile.getContext(), Map.class))
                .orElseGet(HashMap::new);
        // Mirror doCreateDefault email derivation from username
        context.put(
                "email",
                ValidationUtil.patternMatches(next, RegexPatternConstant.EMAIL_PATTERN) ? next : "INVALID EMAIL"
        );
        profile.setContext(context);
        if (current.equalsIgnoreCase(StringUtils.defaultString(profile.getAlias()))) {
            profile.setAlias(next);
        }
        userProfileRepository.saveAndFlush(profile);
    }

    @Transactional
    public int testingDeleteAll() {
        List<User> nonAdminUsers = userRepository.findByUsernameNotInIgnoreCase(List.of("admin@tgt.gg", "ios-tester@tgt.gg"));
        nonAdminUsers.forEach(this::_doHardDeleteExecution);
        return nonAdminUsers.size();
    }

    @Transactional
    public void deleteByUserId(String id, Boolean isSoftDelete) {
        log.info("-- {}.delete => {}", this.getClass().getName(), id);
        User user = uaaService.getById(id);
        if (isSoftDelete) {
            this._doSoftDeleteExecution(user);
        } else {
            this._doHardDeleteExecution(user);
        }
    }

    @Transactional
    public void deleteByIdentifier(String identifier, Boolean isSoftDelete) {
        log.info("-- {}.delete => {}", this.getClass().getName(), identifier);
        User user = uaaService.getUserFromIdentifier(identifier);
        String _userId = String.valueOf(user.getId());
        if (isSoftDelete) {
            this._doSoftDeleteExecution(user);
        } else {
            this._doHardDeleteExecution(user);
        }

    }

    @Transactional
    private void _doSoftDeleteExecution(User user) {
        this._assertDeletable(user);
        if (!user.getIsActive()) {
            return;
        }
        for (Authentication authentication : user.getAuthentications()) {
            authentication.setIsActive(false);
        }
        user.setIsActive(false);
        userRepository.saveAndFlush(user);
        log.info("-- userRepository.soft delete end => {}", user.getId());

        // ==== prepare actionBy
        try {
            GetUserResponseDto actionUser = uaaService.get(Long.valueOf(JwtUtil.userId()));
            kafkaUtil.send(
                    KafkaTopic.USER_STATE_CHANGED,
                    UserStateMutatedEvent.builder()
                            .userId(String.valueOf(user.getId()))
                            .username(user.getUsername())
                            .action("doDeleteExecution::softDelete")
                            .actionBy(AuditAwareUser.builder()
                                    .id(actionUser.getId())
                                    .name(actionUser.getUsername())
                                    .build())
                            .eventName(KafkaTopic.USER_STATE_CHANGED)
                            .requestId(UUID.randomUUID().toString())
                            .build()
            );
        } catch (Exception exception) {
            log.info("=========== This is no longer existed user => ".concat(exception.getMessage()));
        }
    }

    @Transactional
    private void _doHardDeleteExecution(User user) {
        this._assertDeletable(user);
        List<Long> authenticationIds = user.getAuthentications().stream().map(Authentication::getId).toList();
        List<UserRoute> userRoutes = userRouteRepository.findAllByUserId(user.getId());

        userPermissionRepository.deleteByUserId(user.getId());
        userDeviceRepository.deleteByUserId(user.getId());
        userProfileRepository.deleteByUserId(user.getId());
        userPreferenceRepository.deleteByUserId(user.getId());
        userTokenRepository.deleteByUserId(user.getId());
        userVerificationRepository.deleteByUserId(user.getId());
        userRouteRepository.deleteAllById(userRoutes.stream().map(UserRoute::getId).toList());
        authenticationRepository.deleteAllById(authenticationIds);
        userRepository.deleteById(user.getId());
        log.info("-- userRepository.hard delete end => {}", user.getId());

        // Always publish so downstream (profile / program) cleanup still runs for system-token deletes.
        AuditAwareUser actionBy = this._resolveActionBy();
        kafkaUtil.send(
                KafkaTopic.USER_STATE_CHANGED,
                UserStateMutatedEvent.builder()
                        .userId(String.valueOf(user.getId()))
                        .username(user.getUsername())
                        .action("doDeleteExecution")
                        .actionBy(actionBy)
                        .eventName(KafkaTopic.USER_STATE_CHANGED)
                        .requestId(UUID.randomUUID().toString())
                        .build()
        );
        log.info("-- kafkaUtil.send end => {}", user.getId());
    }

    private AuditAwareUser _resolveActionBy() {
        try {
            GetUserResponseDto actionUser = uaaService.get(Long.valueOf(JwtUtil.userId()));
            return AuditAwareUser.builder()
                    .id(actionUser.getId())
                    .name(actionUser.getUsername())
                    .build();
        } catch (Exception exception) {
            log.warn("Could not resolve actionBy for delete event; using system fallback => {}", exception.getMessage());
            return AuditAwareUser.builder()
                    .id("system")
                    .name("system")
                    .build();
        }
    }

    public GetUserResponseDto updateStatuses(UpdateUserStatusRequestDto requestDto, String identifier) {
        User user = uaaService.getUserFromIdentifier(identifier);
        user.setStatus(UserStatus.get(requestDto.getStatus()));
        user = userRepository.save(user);

        kafkaUtil.send(
                KafkaTopic.USER_STATE_CHANGED,
                UserStateMutatedEvent.builder()
                        .userId(String.valueOf(user.getId()))
                        .username(identifier)
                        .eventName(KafkaTopic.USER_STATE_CHANGED)
                        .requestId(UUID.randomUUID().toString())
                        .build()
        );

        return DtoWrapper.getUserResponseDto(user, user.getAuthentications());
    }

    public PaginationDto.PaginationDtoBuilder getAllUsers(Pageable pageable, String startDt, String endDt, String tenantId, String query) {
        return uaaService.getAll(pageable, null, null, tenantId, List.of(), List.of(), null, null, null, null);
    }

    private void _assertDeletable(User user) {
        List<String> protectedUsernames = List.of("admin@tgt.gg", "ios-tester@tgt.gg");
        if (protectedUsernames.stream().anyMatch(name -> name.equalsIgnoreCase(user.getUsername()))) {
            throw new BizException(SystemResponse.SAU0403, "Protected account cannot be deleted.");
        }
    }
}
