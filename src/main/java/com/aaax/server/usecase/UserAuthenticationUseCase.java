package com.aaax.server.usecase;

import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.exception.BizException;
import com.aaax.core.utils.IdSplitter;
import com.aaax.server.entity.dto.request.AddLinkedAuthenticationRequestDto;
import com.aaax.server.entity.dto.request.UserAuthenticationCheckRequestDto;
import com.aaax.server.entity.dto.response.GetLinkedAuthenticationResponseDto;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.exception.response.AuthenticationErrorResponse;
import com.aaax.server.repository.AuthenticationRepository;
import com.aaax.server.service.AuthenticationService;
import com.aaax.server.service.UaaService;
import com.aaax.server.validation.UaaValidation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserAuthenticationUseCase {

    private static final Set<LoginType> PASSWORD_LOGIN_TYPES = EnumSet.of(
            LoginType.EMAIL, LoginType.USERNAME, LoginType.MOBILE
    );
    /** Email remains the default TGT ACO — not unlinkable from settings. */
    private static final Set<LoginType> PROTECTED_FROM_UNLINK = EnumSet.of(
            LoginType.EMAIL, LoginType.USERNAME
    );

    private final AuthenticationService authenticationService;
    private final AuthenticationRepository authenticationRepository;
    private final UaaService uaaService;
    private final PasswordEncoder passwordEncoder;
    private final SocialAuthenticationUseCase socialAuthenticationUseCase;

    public boolean authenticate(UserAuthenticationCheckRequestDto dto) {
        String identifier = UaaValidation.toCanonicalIdentifier(dto.getUsername());
        List<Authentication> authentications = authenticationRepository.findAllByIdentifierIgnoreCase(identifier);
        if (authentications.isEmpty()) {
            throw new BizException(AuthenticationErrorResponse.ATH0001);
        }

        Optional<Authentication> foundAuth = authentications.stream()
                .filter(auth -> PASSWORD_LOGIN_TYPES.contains(auth.getLoginType()))
                .findAny();

        if (foundAuth.isEmpty()) {
            throw new BizException(AuthenticationErrorResponse.ATH0001, "Not any match for [auth]");
        }

        return authenticationService.check_password(
                foundAuth.get(),
                dto.isEncrypted() ? authenticationService.decrypt(dto.getCredentials()) : dto.getCredentials()
        );
    }

    /**
     * Link a login method to the current user.
     * Prefer social: {@code provider} + {@code idToken}. Legacy: username + credentials.
     */
    @Transactional
    public void addLinkedAuthentications(String userId, AddLinkedAuthenticationRequestDto dto) {
        log.info(
                "-- my-linked-authentications request userId={} provider={} hasIdToken={} hasUsername={} hasCredentials={}",
                userId,
                dto.getProvider(),
                StringUtils.isNotBlank(dto.getIdToken()),
                StringUtils.isNotBlank(dto.getUsername()),
                StringUtils.isNotBlank(dto.getCredentials())
        );
        User user = uaaService.getById(userId);

        if (StringUtils.isNotBlank(dto.getProvider()) || StringUtils.isNotBlank(dto.getIdToken())) {
            this._linkSocialProvider(user, dto);
            return;
        }
        this._linkPasswordStyle(user, dto);
    }

    /**
     * Same client signal as social login: provider + idToken. Server only verifies and attaches
     * to the current user (no new User, no tokens).
     */
    private void _linkSocialProvider(User user, AddLinkedAuthenticationRequestDto dto) {
        log.info(
                "-- my-linked-authentications social body userId={} provider={} idToken={}",
                user.getId(),
                dto.getProvider(),
                SocialAuthenticationUseCase.summarizeIdToken(dto.getIdToken())
        );
        socialAuthenticationUseCase.linkProviderToUser(user, dto.getProvider(), dto.getIdToken());
    }

    private void _linkPasswordStyle(User user, AddLinkedAuthenticationRequestDto dto) {
        log.info(
                "-- my-linked-authentications password-style body userId={} username={}",
                user.getId(),
                dto.getUsername()
        );
        if (StringUtils.isBlank(dto.getUsername()) || StringUtils.isBlank(dto.getCredentials())) {
            throw new BizException(AuthenticationErrorResponse.ATH0004,
                    "username and credentials required, or provider+idToken for social link.");
        }
        LoginType loginType = UaaValidation.detechLoginType(dto.getUsername());
        String identifier = UaaValidation.toCanonicalIdentifier(dto.getUsername());

        Optional<Authentication> existing =
                authenticationRepository.findByLoginTypeAndIdentifierIgnoreCase(loginType, identifier);
        if (existing.isPresent()) {
            org.hibernate.Hibernate.initialize(existing.get().getUser());
            if (existing.get().getUser().getId().equals(user.getId())) {
                return;
            }
            throw new BizException(AuthenticationErrorResponse.ATH0002);
        }

        Authentication authentication = Authentication.builder()
                .identifier(identifier)
                .user(user)
                .credentials(UaaValidation.check_passwordRequirement(passwordEncoder, dto.getCredentials(), List.of()))
                .loginType(loginType)
                .lastLoginDt(Instant.now())
                .attempts(0)
                .build();
        try {
            authenticationRepository.saveAndFlush(authentication);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new BizException(AuthenticationErrorResponse.ATH0002);
        }
    }

    public List<GetLinkedAuthenticationResponseDto> fetchLinkedAuthentications(String userId) {
        Long uid = IdSplitter.splitToLong(userId);
        User user = uaaService.getById(userId);
        String accountUsername = user.getUsername();
        List<Authentication> authentications = authenticationRepository.findByUser_Id(uid);
        int methodCount = authentications.size();
        return authentications.stream()
                .map(auth -> GetLinkedAuthenticationResponseDto.builder()
                        .loginType(auth.getLoginType().name())
                        .identifier(auth.getIdentifier())
                        .displayEmail(this._displayEmail(auth, accountUsername))
                        .isLinked(true)
                        .isAbleToUnlink(this._canUnlink(auth.getLoginType(), methodCount))
                        .build())
                .toList();
    }

    /**
     * Prefer email-shaped identifiers for UI; for Apple {@code sub} fall back to account email.
     */
    private String _displayEmail(Authentication auth, String accountUsername) {
        String id = auth.getIdentifier();
        if (StringUtils.isNotBlank(id) && id.contains("@")) {
            return id;
        }
        if (StringUtils.isNotBlank(accountUsername) && accountUsername.contains("@")) {
            return accountUsername.toLowerCase(Locale.ROOT);
        }
        return null;
    }

    /**
     * Unlink a social (or non-protected) method. Email/username ACO cannot be unlinked.
     * Cannot remove the last login method.
     */
    @Transactional
    public void unlinkAuthentication(String userId, String loginTypeRaw) {
        LoginType loginType;
        try {
            loginType = LoginType.get(loginTypeRaw.trim());
        } catch (BizException ex) {
            throw new BizException(AuthenticationErrorResponse.ATH0004, "Invalid loginType: " + loginTypeRaw);
        }

        Long uid = IdSplitter.splitToLong(userId);
        List<Authentication> all = authenticationRepository.findByUser_Id(uid);
        Authentication target = all.stream()
                .filter(a -> loginType.equals(a.getLoginType()))
                .findFirst()
                .orElseThrow(() -> new BizException(AuthenticationErrorResponse.ATH0001,
                        "No " + loginType + " method linked."));

        if (!_canUnlink(loginType, all.size())) {
            throw new BizException(AuthenticationErrorResponse.ATH0003,
                    "Cannot unlink " + loginType + " (protected or last method).");
        }

        authenticationRepository.delete(target);
        log.info("-- unlinked {} from user {}", loginType, uid);
    }

    private boolean _canUnlink(LoginType loginType, int totalMethods) {
        if (PROTECTED_FROM_UNLINK.contains(loginType)) {
            return false;
        }
        return totalMethods > 1;
    }
}
