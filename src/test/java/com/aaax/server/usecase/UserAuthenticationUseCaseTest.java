package com.aaax.server.usecase;

import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.exception.BizException;
import com.aaax.server.entity.dto.request.AddLinkedAuthenticationRequestDto;
import com.aaax.server.entity.dto.request.UserAuthenticationCheckRequestDto;
import com.aaax.server.entity.dto.response.GetLinkedAuthenticationResponseDto;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.exception.response.AuthenticationErrorResponse;
import com.aaax.server.repository.AuthenticationRepository;
import com.aaax.server.service.AuthenticationService;
import com.aaax.server.service.UaaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthenticationUseCaseTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private AuthenticationRepository authenticationRepository;
    @Mock
    private UaaService uaaService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SocialAuthenticationUseCase socialAuthenticationUseCase;

    @InjectMocks
    private UserAuthenticationUseCase userAuthenticationUseCase;

    @Test
    @DisplayName("authenticate should check password for matching login type")
    void authenticate_shouldCheckPassword() {
        Authentication auth = Authentication.builder()
                .identifier("user@test.com")
                .loginType(LoginType.EMAIL)
                .credentials("encoded")
                .build();
        when(authenticationRepository.findAllByIdentifierIgnoreCase("user@test.com")).thenReturn(List.of(auth));
        when(authenticationService.check_password(auth, "plain")).thenReturn(true);

        UserAuthenticationCheckRequestDto dto = UserAuthenticationCheckRequestDto.builder()
                .username("User@test.com")
                .credentials("plain")
                .isEncrypted(false)
                .build();

        assertTrue(userAuthenticationUseCase.authenticate(dto));
    }

    @Test
    @DisplayName("authenticate should decrypt credentials when encrypted")
    void authenticate_shouldDecryptWhenEncrypted() {
        Authentication auth = Authentication.builder()
                .identifier("user@test.com")
                .loginType(LoginType.EMAIL)
                .build();
        when(authenticationRepository.findAllByIdentifierIgnoreCase("user@test.com")).thenReturn(List.of(auth));
        when(authenticationService.decrypt("cipher")).thenReturn("plain");
        when(authenticationService.check_password(auth, "plain")).thenReturn(true);

        UserAuthenticationCheckRequestDto dto = UserAuthenticationCheckRequestDto.builder()
                .username("user@test.com")
                .credentials("cipher")
                .isEncrypted(true)
                .build();

        assertTrue(userAuthenticationUseCase.authenticate(dto));
    }

    @Test
    @DisplayName("authenticate should throw when username missing")
    void authenticate_shouldThrowWhenMissing() {
        when(authenticationRepository.findAllByIdentifierIgnoreCase("missing@test.com")).thenReturn(List.of());
        UserAuthenticationCheckRequestDto dto = UserAuthenticationCheckRequestDto.builder()
                .username("missing@test.com")
                .credentials("x")
                .build();
        assertThrows(BizException.class, () -> userAuthenticationUseCase.authenticate(dto));
    }

    @Test
    @DisplayName("authenticate should throw when no password-based login type")
    void authenticate_shouldThrowWhenNoPasswordLoginType() {
        Authentication auth = Authentication.builder()
                .identifier("sub-123")
                .loginType(LoginType.GOOGLE)
                .build();
        when(authenticationRepository.findAllByIdentifierIgnoreCase("sub-123")).thenReturn(List.of(auth));
        UserAuthenticationCheckRequestDto dto = UserAuthenticationCheckRequestDto.builder()
                .username("sub-123")
                .credentials("x")
                .build();
        assertThrows(BizException.class, () -> userAuthenticationUseCase.authenticate(dto));
    }

    @Test
    @DisplayName("addLinkedAuthentications password-style should save new authentication")
    void addLinkedAuthentications_passwordStyle_shouldSave() {
        User user = User.builder().id(1L).username("user@test.com").build();
        when(uaaService.getById("u_1")).thenReturn(user);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded");
        when(authenticationRepository.findByLoginTypeAndIdentifierIgnoreCase(any(), anyString()))
                .thenReturn(Optional.empty());

        userAuthenticationUseCase.addLinkedAuthentications("u_1",
                AddLinkedAuthenticationRequestDto.builder()
                        .username("91234567")
                        .credentials("Password1")
                        .build());

        verify(authenticationRepository).saveAndFlush(argThat(a ->
                a.getLoginType() == LoginType.MOBILE
                        && "91234567".equals(a.getIdentifier())
                        && "encoded".equals(a.getCredentials())));
    }

    @Test
    @DisplayName("addLinkedAuthentications social delegates to same verify/link path as OAuth")
    void addLinkedAuthentications_google_delegatesToSocialUseCase() {
        User user = User.builder().id(1L).username("user@test.com").build();
        when(uaaService.getById("u_1")).thenReturn(user);

        userAuthenticationUseCase.addLinkedAuthentications("u_1",
                AddLinkedAuthenticationRequestDto.builder()
                        .provider("google")
                        .idToken("tok")
                        .build());

        verify(socialAuthenticationUseCase).linkProviderToUser(user, "google", "tok");
    }

    @Test
    @DisplayName("addLinkedAuthentications social surfaces conflict from shared link path")
    void addLinkedAuthentications_google_conflictPropagates() {
        User user = User.builder().id(1L).username("a@test.com").build();
        when(uaaService.getById("u_1")).thenReturn(user);
        doThrow(new BizException(AuthenticationErrorResponse.ATH0002))
                .when(socialAuthenticationUseCase).linkProviderToUser(user, "google", "tok");

        BizException ex = assertThrows(BizException.class, () ->
                userAuthenticationUseCase.addLinkedAuthentications("u_1",
                        AddLinkedAuthenticationRequestDto.builder()
                                .provider("google")
                                .idToken("tok")
                                .build()));
        assertEquals(AuthenticationErrorResponse.ATH0002, ex.getResponse());
    }

    @Test
    @DisplayName("fetchLinkedAuthentications should return dto with canUnlink rules")
    void fetchLinkedAuthentications_shouldReturnDtos() {
        User user = User.builder().id(1L).username("a@test.com").build();
        when(uaaService.getById("u_1")).thenReturn(user);
        when(authenticationRepository.findByUser_Id(1L)).thenReturn(List.of(
                Authentication.builder().loginType(LoginType.EMAIL).identifier("a@test.com").user(user).build(),
                Authentication.builder().loginType(LoginType.GOOGLE).identifier("a@test.com").user(user).build(),
                Authentication.builder().loginType(LoginType.APPLE).identifier("apple.sub.xyz").user(user).build()
        ));

        List<GetLinkedAuthenticationResponseDto> list =
                userAuthenticationUseCase.fetchLinkedAuthentications("u_1");

        assertEquals(3, list.size());
        GetLinkedAuthenticationResponseDto email = list.stream()
                .filter(i -> "EMAIL".equals(i.getLoginType())).findFirst().orElseThrow();
        GetLinkedAuthenticationResponseDto google = list.stream()
                .filter(i -> "GOOGLE".equals(i.getLoginType())).findFirst().orElseThrow();
        GetLinkedAuthenticationResponseDto apple = list.stream()
                .filter(i -> "APPLE".equals(i.getLoginType())).findFirst().orElseThrow();
        assertFalse(email.isAbleToUnlink());
        assertTrue(google.isAbleToUnlink());
        assertEquals("a@test.com", google.getIdentifier());
        assertEquals("a@test.com", google.getDisplayEmail());
        assertEquals("apple.sub.xyz", apple.getIdentifier());
        assertEquals("a@test.com", apple.getDisplayEmail());
    }

    @Test
    @DisplayName("unlinkAuthentication should delete google when not last")
    void unlink_shouldDeleteGoogle() {
        User user = User.builder().id(1L).build();
        Authentication email = Authentication.builder().loginType(LoginType.EMAIL).identifier("a@x.com").user(user).build();
        Authentication google = Authentication.builder().loginType(LoginType.GOOGLE).identifier("a@x.com").user(user).build();
        when(authenticationRepository.findByUser_Id(1L)).thenReturn(List.of(email, google));

        userAuthenticationUseCase.unlinkAuthentication("u_1", "GOOGLE");

        verify(authenticationRepository).delete(eq(google));
    }

    @Test
    @DisplayName("unlinkAuthentication should reject EMAIL")
    void unlink_shouldRejectEmail() {
        User user = User.builder().id(1L).build();
        when(authenticationRepository.findByUser_Id(1L)).thenReturn(List.of(
                Authentication.builder().loginType(LoginType.EMAIL).identifier("a@x.com").user(user).build(),
                Authentication.builder().loginType(LoginType.GOOGLE).identifier("a@x.com").user(user).build()
        ));

        BizException ex = assertThrows(BizException.class,
                () -> userAuthenticationUseCase.unlinkAuthentication("u_1", "EMAIL"));
        assertEquals(AuthenticationErrorResponse.ATH0003, ex.getResponse());
        verify(authenticationRepository, never()).delete(any(Authentication.class));
    }
}
