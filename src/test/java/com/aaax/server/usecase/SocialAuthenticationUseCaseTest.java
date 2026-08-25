package com.aaax.server.usecase;

import com.aaax.core.common.jsonfield.UserMetadata;
import com.aaax.core.constant.enu.LoginType;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.oauth.AppleIdTokenClaims;
import com.aaax.server.oauth.AppleIdTokenVerifier;
import com.aaax.server.repository.AuthenticationRepository;
import com.aaax.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialAuthenticationUseCaseTest {

    private static final String APPLE_SUB = "001234.abcdef.5678";

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationRepository authenticationRepository;
    @Mock
    private RegisterUserUseCase registerUserUseCase;
    @Mock
    private UserProfileUseCase userProfileUseCase;
    @Mock
    private AppleIdTokenVerifier appleIdTokenVerifier;

    @InjectMocks
    private SocialAuthenticationUseCase socialAuthenticationUseCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(socialAuthenticationUseCase, "systemInvoker", "QS");
    }

    @Test
    void loginByAppleOauth_shouldReturnExistingUserBySub() {
        User user = User.builder().id(42L).username("user@icloud.com").build();
        Authentication appleAuth = Authentication.builder()
                .loginType(LoginType.APPLE)
                .identifier(APPLE_SUB)
                .user(user)
                .build();
        when(appleIdTokenVerifier.verify("token")).thenReturn(AppleIdTokenClaims.builder()
                .sub(APPLE_SUB)
                .build());
        when(authenticationRepository.findByLoginTypeAndIdentifier(LoginType.APPLE, APPLE_SUB))
                .thenReturn(Optional.of(appleAuth));

        User result = socialAuthenticationUseCase.loginByAppleOauth("token", "IOS");

        assertEquals(42L, result.getId());
        verify(registerUserUseCase, never()).executeFrom3rdParty(any(), any(), any(), any());
    }

    @Test
    void loginByAppleOauth_shouldUpdateProfileForExistingUserWhenEmailPresent() {
        User user = User.builder().id(42L).username("acekayin@gmail.com").build();
        Authentication appleAuth = Authentication.builder()
                .loginType(LoginType.APPLE)
                .identifier(APPLE_SUB)
                .user(user)
                .build();
        when(appleIdTokenVerifier.verify("token")).thenReturn(AppleIdTokenClaims.builder()
                .sub(APPLE_SUB)
                .email("acekayin@gmail.com")
                .build());
        when(authenticationRepository.findByLoginTypeAndIdentifier(LoginType.APPLE, APPLE_SUB))
                .thenReturn(Optional.of(appleAuth));
        when(userProfileUseCase._defaultMetadataJson()).thenReturn(new java.util.HashMap<>());

        User result = socialAuthenticationUseCase.loginByAppleOauth("token", "IOS");

        assertEquals(42L, result.getId());
        verify(userProfileUseCase).updateUserProfile(eq("42"), any(), eq("acekayin@gmail.com"), eq("QS"));
    }

    @Test
    void loginByAppleOauth_shouldNotOverwriteRealEmailWithPrivateRelay() {
        User user = User.builder().id(42L).username("acekayin@gmail.com").build();
        Authentication appleAuth = Authentication.builder()
                .loginType(LoginType.APPLE)
                .identifier(APPLE_SUB)
                .user(user)
                .build();
        when(appleIdTokenVerifier.verify("token")).thenReturn(AppleIdTokenClaims.builder()
                .sub(APPLE_SUB)
                .email("p49nw6ybzj@privaterelay.appleid.com")
                .build());
        when(authenticationRepository.findByLoginTypeAndIdentifier(LoginType.APPLE, APPLE_SUB))
                .thenReturn(Optional.of(appleAuth));

        User result = socialAuthenticationUseCase.loginByAppleOauth("token", "IOS");

        assertEquals(42L, result.getId());
        verify(userProfileUseCase, never()).updateUserProfile(any(), any(), any(), any());
    }

    @Test
    void loginByAppleOauth_shouldCreateUserWithEmailWhenFirstLogin() {
        when(appleIdTokenVerifier.verify("token")).thenReturn(AppleIdTokenClaims.builder()
                .sub(APPLE_SUB)
                .email("User@Privaterelay.appleid.com")
                .emailVerified(true)
                .build());

        User created = User.builder().id(7L).username("user@privaterelay.appleid.com").build();
        Authentication createdAppleAuth = Authentication.builder()
                .loginType(LoginType.APPLE)
                .identifier(APPLE_SUB)
                .user(created)
                .build();
        // First-login lookup is empty; after register the same identity is already on the new user
        // so ensureSocialAuthOnUser is idempotent (does not throw ATH0002).
        when(authenticationRepository.findByLoginTypeAndIdentifier(LoginType.APPLE, APPLE_SUB))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(createdAppleAuth));

        when(userRepository.findByUsername("user@privaterelay.appleid.com")).thenReturn(Optional.empty());
        when(registerUserUseCase.executeFrom3rdParty(
                eq("user@privaterelay.appleid.com"),
                eq(APPLE_SUB),
                eq(LoginType.APPLE),
                any(UserMetadata.class)
        )).thenReturn(created);
        when(userProfileUseCase._defaultMetadataJson()).thenReturn(new java.util.HashMap<>());
        when(userProfileUseCase.getUserProfile("7")).thenReturn(
                com.aaax.core.entity.dto.uaa.response.GetUserProfileResponseDto.builder()
                        .context(java.util.Map.of())
                        .build()
        );

        User result = socialAuthenticationUseCase.loginByAppleOauth("token", "IOS");

        assertEquals(7L, result.getId());
        verify(registerUserUseCase).executeFrom3rdParty(
                eq("user@privaterelay.appleid.com"),
                eq(APPLE_SUB),
                eq(LoginType.APPLE),
                any(UserMetadata.class)
        );
        verify(userProfileUseCase).updateUserProfile(eq("7"), any(), eq("user@privaterelay.appleid.com"), eq("QS"));
    }

    @Test
    void loginByAppleOauth_shouldCreateUserWithoutEmailUsingAppleSubUsername() {
        when(appleIdTokenVerifier.verify("token")).thenReturn(AppleIdTokenClaims.builder()
                .sub(APPLE_SUB)
                .build());

        String username = "apple_" + APPLE_SUB;
        User created = User.builder().id(9L).username(username).build();
        Authentication createdAppleAuth = Authentication.builder()
                .loginType(LoginType.APPLE)
                .identifier(APPLE_SUB)
                .user(created)
                .build();
        when(authenticationRepository.findByLoginTypeAndIdentifier(LoginType.APPLE, APPLE_SUB))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(createdAppleAuth));

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(registerUserUseCase.executeFrom3rdParty(
                eq(username),
                eq(APPLE_SUB),
                eq(LoginType.APPLE),
                any(UserMetadata.class)
        )).thenReturn(created);

        User result = socialAuthenticationUseCase.loginByAppleOauth("token", "IOS");

        assertEquals(9L, result.getId());
        verify(userProfileUseCase, never()).updateUserProfile(any(), any(), any(), any());
    }
}
