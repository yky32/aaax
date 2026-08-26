package com.aaax.server.usecase;

import com.aaax.core.api.UtilApiClient;
import com.aaax.core.common.jsonfield.UserMetadata;
import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.enu.KafkaTopic;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.entity.dto.json_context.OtpMetadata;
import com.aaax.server.entity.dto.request.RegisterUserRequestDto;
import com.aaax.server.entity.dto.request.UpdatePasswordRequestDto;
import com.aaax.server.entity.dto.response.PendingVerifyUserResponseDto;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.exception.response.OtpErrorResponse;
import com.aaax.server.exception.response.UaaErrorResponse;
import com.aaax.server.repository.AuthenticationRepository;
import com.aaax.server.repository.UserRepository;
import com.aaax.server.service.AuthenticationService;
import com.aaax.server.service.UaaService;
import com.aaax.server.usecase.otp.RegisterUserOtpUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationRepository authenticationRepository;
    @Mock
    private KafkaUtil kafkaUtil;
    @Mock
    private RedisUtil redisUtil;
    @Mock
    private UaaService uaaService;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private RegisterUserOtpUseCase registerUserOtpUseCase;
    @Mock
    private SystemConfigurationUseCase systemConfigurationUseCase;
    @Mock
    private UserProfileUseCase userProfileUseCase;
    @Mock
    private UserPreferenceUseCase userPreferenceUseCase;
    @Mock
    private UtilApiClient utilApiClient;

    @InjectMocks
    private RegisterUserUseCase registerUserUseCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(registerUserUseCase, "systemInvoker", "test-system");
        ReflectionTestUtils.setField(registerUserUseCase, "userCreatedWaitingTimeMs", 1L);
    }

    // ==================== detectAlias Tests ====================

    @ParameterizedTest
    @CsvSource({
            "yky32is@gmail.com, yky32is",
            "68215274, 68215274",
            "test.user@example.com, test.user",
            "user@domain.org, user",
            "simple@email.com, simple",
            "12345678, 12345678",
            "+1234567890, +1234567890",
            "  yky32is@gmail.com  , yky32is",
            "  68215274  , 68215274"
    })
    @DisplayName("detectAlias should correctly extract alias from username")
    void detectAlias_shouldExtractAliasCorrectly(String input, String expected) {
        String result = (String) ReflectionTestUtils.invokeMethod(
                registerUserUseCase, "detectAlias", input);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("detectAlias should handle email with multiple @ symbols")
    void detectAlias_shouldHandleEmailWithMultipleAtSymbols() {
        String result = (String) ReflectionTestUtils.invokeMethod(
                registerUserUseCase, "detectAlias", "user@domain@example.com");
        assertEquals("user", result);
    }

    @Test
    @DisplayName("detectAlias should throw when username is null")
    void detectAlias_shouldThrowWhenUsernameIsNull() {
        assertThrows(BizException.class, () -> {
            ReflectionTestUtils.invokeMethod(registerUserUseCase, "detectAlias", (String) null);
        });
    }

    @Test
    @DisplayName("detectAlias should handle empty string")
    void detectAlias_shouldHandleEmptyString() {
        String result = (String) ReflectionTestUtils.invokeMethod(
                registerUserUseCase, "detectAlias", "");
        assertEquals("", result);
    }

    // ==================== verify Tests ====================

    @Test
    @DisplayName("verify should delegate to registerUserOtpUseCase")
    void verify_shouldDelegateToOtpUseCase() {
        RegisterUserRequestDto requestDto = RegisterUserRequestDto.builder()
                .username("test@test.com").code("123456").build();

        when(registerUserOtpUseCase.verify(any())).thenReturn(true);

        boolean result = registerUserUseCase.verify(requestDto);

        assertTrue(result);
        verify(registerUserOtpUseCase).verify(argThat(dto ->
                dto.getTo().equals("test@test.com") &&
                        dto.getCode().equals("123456")
        ));
    }

    // ==================== register_public Tests ====================

    @Test
    @DisplayName("register_public should throw when username already verified")
    void register_public_shouldThrowWhenAlreadyVerified() {
        RegisterUserRequestDto requestDto = RegisterUserRequestDto.builder()
                .username("test@test.com").credentials("password123").build();

        when(redisUtil.hasKey(anyString())).thenReturn(true);

        BizException exception = assertThrows(BizException.class, () -> {
            registerUserUseCase.register_public(requestDto);
        });

        assertEquals(OtpErrorResponse.OTP2001.getCode(), exception.getResponse().getCode());
    }

    @Test
    @DisplayName("register_public should succeed for new user")
    void register_public_shouldSucceedForNewUser() {
        RegisterUserRequestDto requestDto = RegisterUserRequestDto.builder()
                .username("newuser@test.com").credentials("password123").sourceSystem("test-system").build();

        when(redisUtil.hasKey(anyString())).thenReturn(false);
        doNothing().when(authenticationService).isThisUsernameExistedForPublicRegister(anyString());
        when(registerUserOtpUseCase.generate(any(), anyString())).thenReturn(
                OtpMetadata.builder().code("otp-123").counter(3).ttl(120).build());

        PendingVerifyUserResponseDto result = registerUserUseCase.register_public(requestDto);

        assertNotNull(result);
        verify(registerUserOtpUseCase).markAsOccupied(anyString());
        verify(registerUserOtpUseCase).generate(any(), eq("public-user-register"));
    }

    @Test
    @DisplayName("register_public_checkOnly free → 200, no OTP")
    void register_public_checkOnly_free_noOtp() {
        RegisterUserRequestDto requestDto = RegisterUserRequestDto.builder()
                .username("NewUser@test.com").credentials("password123").build();
        doNothing().when(authenticationService).isThisUsernameExistedForPublicRegister(anyString());

        PendingVerifyUserResponseDto result = registerUserUseCase.register_public_checkOnly(requestDto);

        assertEquals("newuser@test.com", result.getUsername());
        assertNull(result.getCode());
        verify(registerUserOtpUseCase, never()).generate(any(), anyString());
        verify(registerUserOtpUseCase, never()).markAsOccupied(anyString());
        verify(authenticationService).isThisUsernameExistedForPublicRegister("newuser@test.com");
    }

    @Test
    @DisplayName("register_public_checkOnly occupied → UAA0409")
    void register_public_checkOnly_occupied() {
        RegisterUserRequestDto requestDto = RegisterUserRequestDto.builder()
                .username("taken@test.com").build();
        doThrow(new BizException(UaaErrorResponse.UAA0409))
                .when(authenticationService).isThisUsernameExistedForPublicRegister(anyString());

        BizException ex = assertThrows(BizException.class,
                () -> registerUserUseCase.register_public_checkOnly(requestDto));
        assertEquals(UaaErrorResponse.UAA0409.getCode(), ex.getResponse().getCode());
        verify(registerUserOtpUseCase, never()).generate(any(), anyString());
    }

    // ==================== execute Tests ====================

    @Test
    @DisplayName("execute should create new user successfully")
    void execute_shouldCreateNewUserSuccessfully() {
        RegisterUserRequestDto requestDto = RegisterUserRequestDto.builder()
                .username("newuser@test.com").credentials("password123")
                .sourceSystem("test-system").extraFeatures(Collections.emptyList())
                .metadata(Map.of("phone", "12345678")).build();

        User savedUser = User.builder().id(1L).username("newuser@test.com")
                .status(UserStatus.ACTIVE).sourceSystemTags(List.of("test-system")).build();

        Authentication auth = Authentication.builder().identifier("newuser@test.com")
                .user(savedUser).loginType(LoginType.EMAIL).build();
        savedUser.setAuthentications(List.of(auth));

        when(authenticationService.findOptionalByDynamicIdentifier(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);

        GetUserResponseDto result = registerUserUseCase.execute(requestDto);

        assertNotNull(result);
        verify(userRepository).saveAndFlush(any(User.class));
        verify(kafkaUtil).send(eq(KafkaTopic.USER_CREATED), any());
        verify(userProfileUseCase).doCreateDefault(any(), anyLong());
        verify(userPreferenceUseCase).doCreateDefault(anyString(), isNull());
    }

    @Test
    @DisplayName("execute should throw when user already exists and is ACTIVE")
    void execute_shouldThrowWhenUserAlreadyExistsAndActive() {
        RegisterUserRequestDto requestDto = RegisterUserRequestDto.builder()
                .username("existing@test.com").credentials("password123")
                .extraFeatures(Collections.emptyList()).build();

        User existingUser = User.builder().id(1L).username("existing@test.com")
                .status(UserStatus.ACTIVE).build();
        Authentication auth = Authentication.builder().identifier("existing@test.com")
                .user(existingUser).build();

        when(authenticationService.findOptionalByDynamicIdentifier(anyString())).thenReturn(Optional.of(auth));
        when(uaaService.getById(1L)).thenReturn(existingUser);

        BizException exception = assertThrows(BizException.class, () -> {
            registerUserUseCase.execute(requestDto, UserStatus.ACTIVE, UserMetadata.builder().build());
        });

        assertEquals(UaaErrorResponse.UAA0409.getCode(), exception.getResponse().getCode());
    }

    @Test
    @DisplayName("execute should throw when user is INACTIVE")
    void execute_shouldThrowWhenUserIsInactive() {
        RegisterUserRequestDto requestDto = RegisterUserRequestDto.builder()
                .username("inactive@test.com").credentials("password123")
                .extraFeatures(Collections.emptyList()).build();

        User inactiveUser = User.builder().id(1L).username("inactive@test.com")
                .status(UserStatus.INACTIVE).build();
        Authentication auth = Authentication.builder().identifier("inactive@test.com")
                .user(inactiveUser).build();

        when(authenticationService.findOptionalByDynamicIdentifier(anyString())).thenReturn(Optional.of(auth));
        when(uaaService.getById(1L)).thenReturn(inactiveUser);

        BizException exception = assertThrows(BizException.class, () -> {
            registerUserUseCase.execute(requestDto, UserStatus.ACTIVE, UserMetadata.builder().build());
        });

        assertEquals(UaaErrorResponse.UAA0004.getCode(), exception.getResponse().getCode());
    }

    @Test
    @DisplayName("execute should activate PENDING_VERIFY user")
    void execute_shouldActivatePendingVerifyUser() {
        RegisterUserRequestDto requestDto = RegisterUserRequestDto.builder()
                .username("pending@test.com").credentials("password123")
                .extraFeatures(Collections.emptyList()).build();

        User pendingUser = User.builder().id(1L).username("pending@test.com")
                .status(UserStatus.PENDING_VERIFY).build();
        Authentication auth = Authentication.builder().identifier("pending@test.com")
                .user(pendingUser).build();
        pendingUser.setAuthentications(List.of(auth));

        when(authenticationService.findOptionalByDynamicIdentifier(anyString())).thenReturn(Optional.of(auth));
        when(uaaService.getById(1L)).thenReturn(pendingUser);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(pendingUser);

        GetUserResponseDto result = registerUserUseCase.execute(
                requestDto, UserStatus.ACTIVE, UserMetadata.builder().build());

        assertNotNull(result);
        assertEquals(UserStatus.ACTIVE, pendingUser.getStatus());
        verify(userRepository).saveAndFlush(pendingUser);
    }

    // ==================== execute_external Tests ====================

    @Test
    @DisplayName("execute_external should throw when username is blank")
    void execute_external_shouldThrowWhenUsernameBlank() {
        RegisterUserRequestDto requestDto = RegisterUserRequestDto.builder()
                .username("").credentials("password123").build();

        assertThrows(BizException.class, () -> {
            registerUserUseCase.execute_external(requestDto);
        });
    }

    @Test
    @DisplayName("execute_external should throw when credentials is blank")
    void execute_external_shouldThrowWhenCredentialsBlank() {
        RegisterUserRequestDto requestDto = RegisterUserRequestDto.builder()
                .username("test@test.com").credentials("").build();

        assertThrows(BizException.class, () -> {
            registerUserUseCase.execute_external(requestDto);
        });
    }

    @Test
    @DisplayName("execute_external should throw when no verified key in Redis")
    void execute_external_shouldThrowWhenNoVerifiedKey() {
        RegisterUserRequestDto requestDto = RegisterUserRequestDto.builder()
                .username("test@test.com").credentials("password123").build();

        when(redisUtil.hasKey(anyString())).thenReturn(false);

        assertThrows(BizException.class, () -> {
            registerUserUseCase.execute_external(requestDto);
        });
    }

    @Test
    @DisplayName("execute_external should delete redis key after successful registration")
    void execute_external_shouldDeleteRedisKeyAfterSuccess() {
        RegisterUserRequestDto requestDto = RegisterUserRequestDto.builder()
                .username("test@test.com").credentials("password123")
                .extraFeatures(Collections.emptyList()).build();

        User savedUser = User.builder().id(1L).username("test@test.com")
                .status(UserStatus.ACTIVE).build();
        Authentication auth = Authentication.builder().identifier("test@test.com")
                .user(savedUser).build();
        savedUser.setAuthentications(List.of(auth));

        when(redisUtil.hasKey(anyString())).thenReturn(true);
        when(authenticationService.findOptionalByDynamicIdentifier(anyString())).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);

        registerUserUseCase.execute_external(requestDto);

        verify(redisUtil).delete(anyString());
    }

    // ==================== executeFrom3rdParty Tests ====================

    @Test
    @DisplayName("executeFrom3rdParty should create user with third party authentication")
    void executeFrom3rdParty_shouldCreateUserWithThirdPartyAuth() {
        String username = "google-user@gmail.com";
        UserMetadata metadata = UserMetadata.builder().build();

        User savedUser = User.builder().id(1L).username(username)
                .status(UserStatus.ACTIVE).metadata(metadata)
                .sourceSystemTags(List.of("test-system")).build();

        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);

        User result = registerUserUseCase.executeFrom3rdParty(username, LoginType.EMAIL, metadata);

        assertNotNull(result);
        verify(userRepository).saveAndFlush(argThat(user ->
                user.getUsername().equals(username.toLowerCase()) &&
                        user.getStatus() == UserStatus.ACTIVE &&
                        user.getAuthentications().size() == 2
        ));
    }

    @Test
    @DisplayName("executeFrom3rdParty should create user with phone number")
    void executeFrom3rdParty_shouldCreateUserWithPhone() {
        String username = "+85212345678";
        UserMetadata metadata = UserMetadata.builder().build();
        User savedUser = User.builder().id(2L).username(username)
                .status(UserStatus.ACTIVE).build();

        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);

        User result = registerUserUseCase.executeFrom3rdParty(username, LoginType.MOBILE, metadata);

        assertNotNull(result);
        verify(userRepository).saveAndFlush(argThat(user ->
                        user.getAuthentications().size() == 2
        ));
    }

    // ==================== updateCredentials Tests ====================

    @Test
    @DisplayName("updateCredentials should update password successfully")
    void updateCredentials_shouldUpdatePasswordSuccessfully() {
        String identifier = "user@test.com";
        UpdatePasswordRequestDto dto = new UpdatePasswordRequestDto();
        dto.setExistingCredentials("oldPassword");
        dto.setCredentials("newPassword");

        User user = User.builder().id(1L).username(identifier).build();
        Authentication auth = Authentication.builder().identifier(identifier)
                .credentials("encodedOldPassword").user(user).build();
        user.setAuthentications(List.of(auth));

        when(uaaService.getByUsername(identifier)).thenReturn(auth);
        when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("newPassword", "encodedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(authenticationRepository.save(any(Authentication.class))).thenReturn(auth);

        var result = registerUserUseCase.updateCredentials(dto, identifier);

        assertNotNull(result);
        verify(authenticationRepository).save(auth);
        assertEquals("encodedNewPassword", auth.getCredentials());
    }

    @Test
    @DisplayName("updateCredentials should throw when existing password is incorrect")
    void updateCredentials_shouldThrowWhenExistingPasswordIncorrect() {
        String identifier = "user@test.com";
        UpdatePasswordRequestDto dto = new UpdatePasswordRequestDto();
        dto.setExistingCredentials("wrongPassword");
        dto.setCredentials("newPassword");

        User user = User.builder().id(1L).username(identifier).build();
        Authentication auth = Authentication.builder().identifier(identifier)
                .credentials("encodedPassword").user(user).build();

        when(uaaService.getByUsername(identifier)).thenReturn(auth);
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(BizException.class, () -> {
            registerUserUseCase.updateCredentials(dto, identifier);
        });
    }

    @Test
    @DisplayName("updateCredentials should throw when new password is same as old")
    void updateCredentials_shouldThrowWhenNewPasswordSameAsOld() {
        String identifier = "user@test.com";
        UpdatePasswordRequestDto dto = new UpdatePasswordRequestDto();
        dto.setExistingCredentials("password123");
        dto.setCredentials("password123");

        User user = User.builder().id(1L).username(identifier).build();
        Authentication auth = Authentication.builder().identifier(identifier)
                .credentials("encodedPassword").user(user).build();

        when(uaaService.getByUsername(identifier)).thenReturn(auth);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        assertThrows(BizException.class, () -> {
            registerUserUseCase.updateCredentials(dto, identifier);
        });
    }

    @Test
    @DisplayName("updateCredentials should not update when credentials is null")
    void updateCredentials_shouldNotUpdateWhenCredentialsNull() {
        String identifier = "user@test.com";
        UpdatePasswordRequestDto dto = new UpdatePasswordRequestDto();
        dto.setCredentials(null);

        User user = User.builder().id(1L).username(identifier).build();
        Authentication auth = Authentication.builder().identifier(identifier)
                .credentials("encodedPassword").user(user).build();
        user.setAuthentications(List.of(auth));

        when(uaaService.getByUsername(identifier)).thenReturn(auth);

        var result = registerUserUseCase.updateCredentials(dto, identifier);

        assertNotNull(result);
        verify(authenticationRepository, never()).save(any());
    }


    // ==================== checkUsernameExisted Tests ====================

    @Test
    @DisplayName("checkUsernameExisted should call authenticationService")
    void checkUsernameExisted_shouldCallAuthenticationService() {
        String username = "test@test.com";
        doNothing().when(authenticationService).isThisUsernameExistedForPublicRegister(username);

        ReflectionTestUtils.invokeMethod(registerUserUseCase, "checkUsernameExisted", username);

        verify(authenticationService).isThisUsernameExistedForPublicRegister(username);
    }
}
