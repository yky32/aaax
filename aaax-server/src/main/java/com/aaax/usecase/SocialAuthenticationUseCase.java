package com.aaax.usecase;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.aaax.core.common.jsonfield.UserMetadata;
import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.entity.dto.uaa.response.GetUserProfileResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.utils.JSONUtil;
import com.aaax.entity.dto.request.UpdateUserProfileRequestDto;
import com.aaax.entity.po.user.Authentication;
import com.aaax.entity.po.user.User;
import com.aaax.exception.response.AuthenticationErrorResponse;
import com.aaax.exception.response.UaaErrorResponse;
import com.aaax.oauth.AppleIdTokenClaims;
import com.aaax.oauth.AppleIdTokenVerifier;
import com.aaax.repository.AuthenticationRepository;
import com.aaax.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Google / Apple idToken verification and social auth attachment.
 * <p>
 * Client always sends an idToken (login screen or settings “Link”). Server always verifies first.
 * <ul>
 *   <li><b>Login</b> — resolve identity → find/create user → ensure auth row → issue tokens (caller)</li>
 *   <li><b>Link</b> — resolve identity → attach auth row to <em>current</em> user only (no new user)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SocialAuthenticationUseCase {

    private final UserRepository userRepository;
    private final AuthenticationRepository authenticationRepository;
    private final RegisterUserUseCase registerUserUseCase;
    private final UserProfileUseCase userProfileUseCase;
    private final AppleIdTokenVerifier appleIdTokenVerifier;
    @Value("${config.system-invoker}")
    protected String systemInvoker;
    @Value("${oauth-provider.google.web-app.client-id}")
    private String webAppGoogleClientId;
    @Value("${oauth-provider.google.ios.client-id}")
    private String iosGoogleClientId;
    @Value("${oauth-provider.google.android.client-id}")
    private String androidGoogleClientId;

    /**
     * Verified social identity shared by login and link.
     *
     * @param loginType  GOOGLE / APPLE
     * @param identifier stored on {@code authentications.identifier} (Google: email, Apple: sub)
     * @param email      email from token when present (display / profile)
     * @param mapUsername username used when creating/mapping a User on login
     */
    public record SocialIdentity(
            LoginType loginType,
            String identifier,
            String email,
            String mapUsername,
            String givenName,
            String familyName,
            String pictureUrl,
            String locale
    ) {}

    // -------------------------------------------------------------------------
    // Shared: client idToken → verified identity (login AND link)
    // -------------------------------------------------------------------------

    /**
     * Structured request log for both login and link (same client payload shape).
     * idToken is logged truncated — full JWT is not written to avoid secret sprawl in log drains.
     */
    public void logSocialAuthRequest(String flow, String provider, String idToken, String userId, String deviceType) {
        log.info(
                "-- social-auth request flow={} provider={} userId={} deviceType={} idToken={}",
                flow,
                provider,
                userId,
                deviceType,
                summarizeIdToken(idToken)
        );
    }

    static String summarizeIdToken(String idToken) {
        if (idToken == null) {
            return "null";
        }
        String t = idToken.trim();
        if (t.isEmpty()) {
            return "empty";
        }
        int len = t.length();
        if (len <= 24) {
            return "len=" + len + " value=" + t;
        }
        return "len=" + len + " prefix=" + t.substring(0, 12) + "…suffix=" + t.substring(len - 8);
    }

    public SocialIdentity resolveProviderIdentity(String provider, String idToken) {
        if (StringUtils.isBlank(provider) || StringUtils.isBlank(idToken)) {
            throw new BizException(AuthenticationErrorResponse.ATH0004,
                    "provider and idToken are required.");
        }
        LoginType loginType;
        try {
            loginType = LoginType.get(provider.trim());
        } catch (BizException ex) {
            throw new BizException(AuthenticationErrorResponse.ATH0004,
                    "Unsupported provider: " + provider);
        }
        SocialIdentity identity = switch (loginType) {
            case GOOGLE -> this.resolveGoogleIdentity(idToken);
            case APPLE -> this.resolveAppleIdentity(idToken);
            default -> throw new BizException(AuthenticationErrorResponse.ATH0004,
                    "Only google and apple are supported via idToken.");
        };
        log.info(
                "-- social-auth verified flow=resolve provider={} identifier={} email={} mapUsername={}",
                identity.loginType(),
                identity.identifier(),
                identity.email(),
                identity.mapUsername()
        );
        return identity;
    }

    public SocialIdentity resolveGoogleIdentity(String idToken) {
        GoogleIdToken.Payload payload = this._verifyGoogleIdToken(idToken);
        String email = payload.getEmail();
        if (StringUtils.isBlank(email)) {
            throw new BizException(UaaErrorResponse.UAA0401, Map.of(
                    "provider", "Google",
                    "reason", "email missing from idToken"
            ));
        }
        if (Boolean.FALSE.equals(payload.getEmailVerified())) {
            throw new BizException(UaaErrorResponse.UAA0401, Map.of(
                    "provider", "Google",
                    "reason", "email not verified on Google account"
            ));
        }
        String identifier = email.toLowerCase(Locale.ROOT);
        log.info(
                "-- social-auth google claims sub={} email={} emailVerified={} name={}",
                payload.getSubject(),
                identifier,
                payload.getEmailVerified(),
                payload.get("name")
        );
        return new SocialIdentity(
                LoginType.GOOGLE,
                identifier,
                identifier,
                identifier,
                (String) payload.get("given_name"),
                (String) payload.get("family_name"),
                (String) payload.get("picture"),
                (String) payload.get("locale")
        );
    }

    public SocialIdentity resolveAppleIdentity(String idToken) {
        AppleIdTokenClaims claims = appleIdTokenVerifier.verify(idToken);
        String sub = claims.getSub();
        if (StringUtils.isBlank(sub)) {
            throw new BizException(UaaErrorResponse.UAA0401, Map.of(
                    "provider", "Apple",
                    "reason", "sub missing from idToken"
            ));
        }
        String email = StringUtils.isNotBlank(claims.getEmail())
                ? claims.getEmail().trim().toLowerCase(Locale.ROOT)
                : null;
        String mapUsername = email != null ? email : "apple_" + sub;
        log.info(
                "-- social-auth apple claims sub={} email={} emailVerified={}",
                sub,
                email,
                claims.getEmailVerified()
        );
        return new SocialIdentity(
                LoginType.APPLE,
                sub,
                email,
                mapUsername,
                null,
                null,
                null,
                null
        );
    }

    // -------------------------------------------------------------------------
    // Shared: attach social auth row to a known user
    // -------------------------------------------------------------------------

    /**
     * Ensure {@code identity} is an {@link Authentication} on {@code user}.
     * If the same provider+identifier is already on another user → {@code ATH0002}.
     * If this user already has a different identity for the same provider → {@code ATH0002}.
     * Concurrent insert races map to {@code ATH0002}.
     */
    @Transactional
    public void ensureSocialAuthOnUser(User user, SocialIdentity identity) {
        Optional<Authentication> existing =
                authenticationRepository.findByLoginTypeAndIdentifier(identity.loginType(), identity.identifier());
        if (existing.isPresent()) {
            Authentication row = existing.get();
            Hibernate.initialize(row.getUser());
            Long ownerId = row.getUser().getId();
            if (ownerId.equals(user.getId())) {
                log.info("-- {} already on user {}", identity.loginType(), user.getId());
                return;
            }
            throw new BizException(AuthenticationErrorResponse.ATH0002,
                    "provider=" + identity.loginType() + ", identifier=" + identity.identifier()
                            + " already owned by another user");
        }

        boolean sameTypeOnUser = authenticationRepository.findByUser_Id(user.getId()).stream()
                .anyMatch(a -> identity.loginType().equals(a.getLoginType()));
        if (sameTypeOnUser) {
            throw new BizException(AuthenticationErrorResponse.ATH0002,
                    "User already has a " + identity.loginType() + " login method linked.");
        }

        Authentication socialAuthentication = Authentication.builder()
                .identifier(identity.identifier())
                .user(user)
                .loginType(identity.loginType())
                .credentials("-")
                .lastLoginDt(Instant.now())
                .attempts(0)
                .build();
        try {
            authenticationRepository.saveAndFlush(socialAuthentication);
        } catch (DataIntegrityViolationException ex) {
            log.warn("-- social auth attach race/conflict userId={} provider={}: {}",
                    user.getId(), identity.loginType(), ex.getMessage());
            throw new BizException(AuthenticationErrorResponse.ATH0002,
                    "provider=" + identity.loginType() + ", identifier=" + identity.identifier());
        }
        log.info("-- attached {} to user {} identifier={}", identity.loginType(), user.getId(), identity.identifier());
    }

    /**
     * Settings “Link Google/Apple”: verify idToken and attach to the logged-in user only.
     * Google (and Apple when email is present and not private-relay) must match account email.
     */
    @Transactional
    public void linkProviderToUser(User user, String provider, String idToken) {
        this.logSocialAuthRequest(
                "LINK",
                provider,
                idToken,
                user != null ? String.valueOf(user.getId()) : null,
                null
        );
        SocialIdentity identity = this.resolveProviderIdentity(provider, idToken);
        this.assertLinkIdentityMatchesAccount(user, identity);
        this.ensureSocialAuthOnUser(user, identity);
        log.info(
                "-- social-auth LINK done userId={} provider={} identifier={}",
                user.getId(),
                identity.loginType(),
                identity.identifier()
        );
    }

    /**
     * Strict ACO: social email must match the email account username when available.
     * Apple private-relay / missing email: allow (identifier is {@code sub}).
     */
    void assertLinkIdentityMatchesAccount(User user, SocialIdentity identity) {
        String account = user.getUsername() == null ? "" : user.getUsername().trim().toLowerCase(Locale.ROOT);
        if (identity.loginType() == LoginType.GOOGLE) {
            if (StringUtils.isBlank(identity.email()) || !account.equals(identity.email())) {
                throw new BizException(AuthenticationErrorResponse.ATH0004,
                        "Google email must match the signed-in account email (" + account + ").");
            }
            return;
        }
        if (identity.loginType() == LoginType.APPLE
                && StringUtils.isNotBlank(identity.email())
                && !_isApplePrivateRelayEmail(identity.email())
                && !account.equals(identity.email())) {
            throw new BizException(AuthenticationErrorResponse.ATH0004,
                    "Apple email must match the signed-in account email (" + account + ").");
        }
    }

    // -------------------------------------------------------------------------
    // Login: same verify, then find/create user + attach
    // -------------------------------------------------------------------------

    @Transactional
    public User loginByGoogleOauth(String idToken, String deviceType) {
        this.logSocialAuthRequest("LOGIN", "google", idToken, null, deviceType);
        SocialIdentity identity = this.resolveGoogleIdentity(idToken);
        log.info(
                "-- social-auth LOGIN google identity identifier={} email={}",
                identity.identifier(),
                identity.email()
        );
        UserMetadata userMetadata = UserMetadata.builder().build();
        User resultUser = this.createOrMapUser(
                identity.mapUsername(),
                identity.identifier(),
                identity.loginType(),
                userMetadata
        );

        Map<String, Object> metadata = userProfileUseCase._defaultMetadataJson();
        metadata.put("firstName", identity.givenName());
        metadata.put("lastName", identity.familyName());
        metadata.put("locale", identity.locale());
        metadata.put("email", identity.email());

        GetUserProfileResponseDto userProfile = userProfileUseCase.getUserProfile(String.valueOf(resultUser.getId()));
        Map context = JSONUtil.convertFromObject(userProfile.getContext(), Map.class);
        if (context != null && !context.isEmpty()) {
            if (context.get("avatar") == null || "".equals(context.get("avatar"))) {
                metadata.put("avatar", identity.pictureUrl());
                UpdateUserProfileRequestDto updateUserProfileRequestDto = UpdateUserProfileRequestDto.builder()
                        .context(metadata)
                        .build();
                userProfileUseCase.updateUserProfile(
                        resultUser.getId().toString(),
                        updateUserProfileRequestDto,
                        identity.identifier(),
                        systemInvoker
                );
            }
        }
        log.info(
                "-- social-auth LOGIN google done userId={} username={}",
                resultUser.getId(),
                resultUser.getUsername()
        );
        return resultUser;
    }

    @Transactional
    public User loginByAppleOauth(String idToken, String deviceType) {
        this.logSocialAuthRequest("LOGIN", "apple", idToken, null, deviceType);
        SocialIdentity identity = this.resolveAppleIdentity(idToken);
        // Keep explicit claims log (merged from main #14) for ops debugging
        log.info(
                "Apple sign-in verified claims: sub={} email={} mapUsername={}",
                identity.identifier(),
                identity.email(),
                identity.mapUsername()
        );
        log.info(
                "-- social-auth LOGIN apple identity identifier={} email={} mapUsername={}",
                identity.identifier(),
                identity.email(),
                identity.mapUsername()
        );

        Optional<Authentication> existingAppleAuth =
                authenticationRepository.findByLoginTypeAndIdentifier(LoginType.APPLE, identity.identifier());
        if (existingAppleAuth.isPresent()) {
            User user = existingAppleAuth.get().getUser();
            this._updateAppleProfileIfNeeded(user, identity.email());
            log.info(
                    "-- social-auth LOGIN apple existing userId={} username={}",
                    user.getId(),
                    user.getUsername()
            );
            return user;
        }

        UserMetadata userMetadata = UserMetadata.builder().build();
        User resultUser = this.createOrMapUser(
                identity.mapUsername(),
                identity.identifier(),
                identity.loginType(),
                userMetadata
        );
        this._updateAppleProfileIfNeeded(resultUser, identity.email());
        log.info(
                "-- social-auth LOGIN apple created/mapped userId={} username={}",
                resultUser.getId(),
                resultUser.getUsername()
        );
        return resultUser;
    }

    private void _updateAppleProfileIfNeeded(User user, String tokenEmail) {
        if (StringUtils.isBlank(tokenEmail)) {
            return;
        }
        if (_isApplePrivateRelayEmail(tokenEmail) && !_isApplePrivateRelayEmail(user.getUsername())) {
            return;
        }
        if (_isApplePrivateRelayEmail(tokenEmail)) {
            GetUserProfileResponseDto userProfile = userProfileUseCase.getUserProfile(String.valueOf(user.getId()));
            if (userProfile != null) {
                Map<?, ?> existingContext = JSONUtil.convertFromObject(userProfile.getContext(), Map.class);
                if (existingContext != null) {
                    Object existingEmail = existingContext.get("email");
                    if (existingEmail instanceof String existingEmailStr
                            && StringUtils.isNotBlank(existingEmailStr)
                            && !_isApplePrivateRelayEmail(existingEmailStr)) {
                        return;
                    }
                }
            }
        }
        Map<String, Object> metadata = userProfileUseCase._defaultMetadataJson();
        metadata.put("email", tokenEmail);
        UpdateUserProfileRequestDto updateUserProfileRequestDto = UpdateUserProfileRequestDto.builder()
                .context(metadata)
                .build();
        userProfileUseCase.updateUserProfile(
                user.getId().toString(),
                updateUserProfileRequestDto,
                user.getUsername(),
                systemInvoker
        );
    }

    private static boolean _isApplePrivateRelayEmail(String email) {
        return email != null && email.endsWith("@privaterelay.appleid.com");
    }

    private GoogleIdToken.Payload _verifyGoogleIdToken(String idToken) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(List.of(webAppGoogleClientId, iosGoogleClientId, androidGoogleClientId))
                .build();
        GoogleIdToken _idToken;
        try {
            _idToken = verifier.verify(idToken);
        } catch (GeneralSecurityException | IOException e) {
            throw new BizException(UaaErrorResponse.UAA0401, Map.of("provider", "Google", "error", e.getMessage()));
        }
        if (_idToken == null) {
            throw new BizException(UaaErrorResponse.UAA0401, "[_idToken] is null.");
        }
        return _idToken.getPayload();
    }

    /**
     * Login path: find user by username or register, then ensure social auth row (same attach as link).
     */
    @Transactional
    User createOrMapUser(String username, String authIdentifier, LoginType loginType, UserMetadata metadata) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .or(() -> userRepository.findByUsername(username))
                .orElseGet(
                () -> registerUserUseCase.executeFrom3rdParty(username, authIdentifier, loginType, metadata)
        );
        SocialIdentity identity = new SocialIdentity(
                loginType,
                authIdentifier,
                loginType == LoginType.GOOGLE ? authIdentifier : null,
                username,
                null,
                null,
                null,
                null
        );
        // After register, auth may already exist; ensureSocialAuthOnUser is idempotent for same user.
        this.ensureSocialAuthOnUser(user, identity);
        return user;
    }
}
