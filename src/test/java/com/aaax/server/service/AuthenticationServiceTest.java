package com.aaax.server.service;

import com.nimbusds.jose.jwk.RSAKey;
import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.exception.BizException;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.entity.dto.json_context.OtpMetadata;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.exception.response.UaaErrorResponse;
import com.aaax.server.repository.AuthenticationRepository;
import com.aaax.server.repository.UserRepository;
import com.aaax.server.usecase.otp.ForgotPasswordOtpUseCase;
import com.aaax.server.utils.CryptographyUtil;
import com.aaax.server.validation.UaaValidation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationRepository authenticationRepository;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private ForgotPasswordOtpUseCase forgotPasswordOtpUseCase;
    @Mock
    private KafkaUtil kafkaUtil;
    @Mock
    private RedisUtil redisUtil;
    @Mock
    private RSAKey rsaKey;

    @InjectMocks
    private AuthenticationService authenticationService;

    // ==================== authenticate Tests ====================

    @Test
    @DisplayName("authenticate should return true when credentials match")
    void authenticate_shouldReturnTrueWhenCredentialsMatch() {
        // Arrange
        String storedCredentials = "encodedPassword";
        String credentials = "plainPassword";
        when(encoder.matches(credentials, storedCredentials)).thenReturn(true);

        // Act
        Boolean result = authenticationService.authenticate(storedCredentials, credentials);

        // Assert
        assertTrue(result);
        verify(encoder).matches(credentials, storedCredentials);
    }

    @Test
    @DisplayName("authenticate should return false when credentials don't match")
    void authenticate_shouldReturnFalseWhenCredentialsDontMatch() {
        // Arrange
        String storedCredentials = "encodedPassword";
        String credentials = "wrongPassword";
        when(encoder.matches(credentials, storedCredentials)).thenReturn(false);

        // Act
        Boolean result = authenticationService.authenticate(storedCredentials, credentials);

        // Assert
        assertFalse(result);
    }

    // ==================== findOptionalByDynamicIdentifier Tests ====================

    @Test
    @DisplayName("findOptionalByDynamicIdentifier should return active authentication for email")
    void findOptionalByDynamicIdentifier_shouldReturnActiveAuthForEmail() {
        // Arrange
        String identifier = "user@test.com";
        User user = User.builder().id(1L).build();
        Authentication auth = Authentication.builder()
                .identifier(identifier)
                .user(user)
                .build();
        auth.setIsActive(true);

        try (MockedStatic<UaaValidation> mockedValidation = mockStatic(UaaValidation.class)) {
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifier(identifier)).thenReturn(identifier);
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifierIfPresent(identifier)).thenReturn(identifier);
            mockedValidation.when(() -> UaaValidation.detechLoginType(identifier)).thenReturn(LoginType.EMAIL);
            when(authenticationRepository.findByIdentifierIgnoreCaseAndLoginType(identifier, LoginType.EMAIL))
                    .thenReturn(Optional.of(auth));

            // Act
            Optional<Authentication> result = authenticationService.findOptionalByDynamicIdentifier(identifier);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(identifier, result.get().getIdentifier());
        }
    }

    @Test
    @DisplayName("findOptionalByDynamicIdentifier should return authentication even when inactive (no active filter)")
    void findOptionalByDynamicIdentifier_shouldReturnInactiveAuth() {
        // Arrange
        String identifier = "user@test.com";
        User user = User.builder().id(1L).build();
        Authentication auth = Authentication.builder()
                .identifier(identifier)
                .user(user)
                .build();
        auth.setIsActive(false);

        try (MockedStatic<UaaValidation> mockedValidation = mockStatic(UaaValidation.class)) {
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifier(identifier)).thenReturn(identifier);
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifierIfPresent(identifier)).thenReturn(identifier);
            mockedValidation.when(() -> UaaValidation.detechLoginType(identifier)).thenReturn(LoginType.EMAIL);
            when(authenticationRepository.findByIdentifierIgnoreCaseAndLoginType(identifier, LoginType.EMAIL))
                    .thenReturn(Optional.of(auth));

            // Act
            Optional<Authentication> result = authenticationService.findOptionalByDynamicIdentifier(identifier);

            // Assert — findOptionalByDynamicIdentifier does NOT filter by isActive
            assertTrue(result.isPresent());
            assertEquals(identifier, result.get().getIdentifier());
        }
    }

    @Test
    @DisplayName("findOptionalByDynamicIdentifier should return empty when not found")
    void findOptionalByDynamicIdentifier_shouldReturnEmptyWhenNotFound() {
        // Arrange
        String identifier = "nonexistent@test.com";

        try (MockedStatic<UaaValidation> mockedValidation = mockStatic(UaaValidation.class)) {
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifier(identifier)).thenReturn(identifier);
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifierIfPresent(identifier)).thenReturn(identifier);
            mockedValidation.when(() -> UaaValidation.detechLoginType(identifier)).thenReturn(LoginType.EMAIL);
            when(authenticationRepository.findByIdentifierIgnoreCaseAndLoginType(identifier, LoginType.EMAIL))
                    .thenReturn(Optional.empty());

            // Act
            Optional<Authentication> result = authenticationService.findOptionalByDynamicIdentifier(identifier);

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "user@test.com, EMAIL",
            "+85212345678, MOBILE",
            "username123, USERNAME"
    })
    @DisplayName("findOptionalByDynamicIdentifier should detect correct login type")
    void findOptionalByDynamicIdentifier_shouldDetectCorrectLoginType(String identifier, LoginType expectedType) {
        // Arrange
        try (MockedStatic<UaaValidation> mockedValidation = mockStatic(UaaValidation.class)) {
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifier(identifier)).thenReturn(identifier);
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifierIfPresent(identifier)).thenReturn(identifier);
            mockedValidation.when(() -> UaaValidation.detechLoginType(identifier)).thenReturn(expectedType);
            when(authenticationRepository.findByIdentifierIgnoreCaseAndLoginType(identifier, expectedType))
                    .thenReturn(Optional.empty());

            // Act
            authenticationService.findOptionalByDynamicIdentifier(identifier);

            // Assert
            verify(authenticationRepository).findByIdentifierIgnoreCaseAndLoginType(identifier, expectedType);
        }
    }

    // ==================== findByDynamicIdentifier Tests ====================

    @Test
    @DisplayName("findByDynamicIdentifier should return active authentication")
    void findByDynamicIdentifier_shouldReturnActiveAuthentication() {
        // Arrange
        String identifier = "user@test.com";
        User user = User.builder().id(1L).build();
        Authentication auth = Authentication.builder()
                .identifier(identifier)
                .user(user)
                .build();
        auth.setIsActive(true);

        try (MockedStatic<UaaValidation> mockedValidation = mockStatic(UaaValidation.class)) {
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifier(identifier)).thenReturn(identifier);
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifierIfPresent(identifier)).thenReturn(identifier);
            mockedValidation.when(() -> UaaValidation.detechLoginType(identifier)).thenReturn(LoginType.EMAIL);
            when(authenticationRepository.findByIdentifierIgnoreCaseAndLoginType(identifier, LoginType.EMAIL))
                    .thenReturn(Optional.of(auth));

            // Act
            Authentication result = authenticationService.findValidRecordsByDynamicIdentifier(identifier);

            // Assert
            assertNotNull(result);
            assertEquals(identifier, result.getIdentifier());
        }
    }

    @Test
    @DisplayName("findByDynamicIdentifier should throw when authentication not found")
    void findByDynamicIdentifier_shouldThrowWhenNotFound() {
        // Arrange
        String identifier = "nonexistent@test.com";

        try (MockedStatic<UaaValidation> mockedValidation = mockStatic(UaaValidation.class)) {
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifier(identifier)).thenReturn(identifier);
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifierIfPresent(identifier)).thenReturn(identifier);
            mockedValidation.when(() -> UaaValidation.detechLoginType(identifier)).thenReturn(LoginType.EMAIL);
            when(authenticationRepository.findByIdentifierIgnoreCaseAndLoginType(identifier, LoginType.EMAIL))
                    .thenReturn(Optional.empty());

            // Act & Assert
            BizException exception = assertThrows(BizException.class, () -> {
                authenticationService.findValidRecordsByDynamicIdentifier(identifier);
            });

            assertEquals(UaaErrorResponse.UAA0001.getCode(), exception.getResponse().getCode());
            assertTrue(exception.toString().contains(identifier));
        }
    }

    @Test
    @DisplayName("findByDynamicIdentifier should throw when authentication is inactive")
    void findByDynamicIdentifier_shouldThrowWhenInactive() {
        // Arrange
        String identifier = "inactive@test.com";
        User user = User.builder().id(1L).build();
        Authentication auth = Authentication.builder()
                .identifier(identifier)
                .user(user)
                .build();
        auth.setIsActive(false);

        try (MockedStatic<UaaValidation> mockedValidation = mockStatic(UaaValidation.class)) {
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifier(identifier)).thenReturn(identifier);
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifierIfPresent(identifier)).thenReturn(identifier);
            mockedValidation.when(() -> UaaValidation.detechLoginType(identifier)).thenReturn(LoginType.EMAIL);
            when(authenticationRepository.findByIdentifierIgnoreCaseAndLoginType(identifier, LoginType.EMAIL))
                    .thenReturn(Optional.of(auth));

            // Act & Assert
            BizException exception = assertThrows(BizException.class, () -> {
                authenticationService.findValidRecordsByDynamicIdentifier(identifier);
            });

            assertEquals(UaaErrorResponse.UAA0001.getCode(), exception.getResponse().getCode());
            assertTrue(exception.toString().contains(identifier));
        }
    }

    // ==================== findByIdentifierWithLoginType Tests ====================

    @Test
    @DisplayName("findByIdentifierWithLoginType should return active authentication")
    void findByIdentifierWithLoginType_shouldReturnActiveAuthentication() {
        // Arrange
        String identifier = "user@test.com";
        LoginType loginType = LoginType.EMAIL;
        User user = User.builder().id(1L).build();
        Authentication auth = Authentication.builder()
                .identifier(identifier)
                .user(user)
                .build();
        auth.setIsActive(true);

        when(authenticationRepository.findByIdentifierIgnoreCaseAndLoginType(identifier, loginType))
                .thenReturn(Optional.of(auth));

        // Act
        Authentication result = authenticationService.findByIdentifierWithLoginType(identifier, loginType);

        // Assert
        assertNotNull(result);
        assertEquals(identifier, result.getIdentifier());
    }

    @Test
    @DisplayName("findByIdentifierWithLoginType should throw when not found")
    void findByIdentifierWithLoginType_shouldThrowWhenNotFound() {
        // Arrange
        String identifier = "nonexistent@test.com";
        LoginType loginType = LoginType.EMAIL;

        when(authenticationRepository.findByIdentifierIgnoreCaseAndLoginType(identifier, loginType))
                .thenReturn(Optional.empty());

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> {
            authenticationService.findByIdentifierWithLoginType(identifier, loginType);
        });

        assertEquals(UaaErrorResponse.UAA0001.getCode(), exception.getResponse().getCode());
        assertTrue(exception.toString().contains(identifier));
    }

    @Test
    @DisplayName("findByIdentifierWithLoginType should throw when inactive")
    void findByIdentifierWithLoginType_shouldThrowWhenInactive() {
        // Arrange
        String identifier = "inactive@test.com";
        LoginType loginType = LoginType.EMAIL;
        User user = User.builder().id(1L).build();
        Authentication auth = Authentication.builder()
                .identifier(identifier)
                .user(user)
                .build();
        auth.setIsActive(false);


        when(authenticationRepository.findByIdentifierIgnoreCaseAndLoginType(identifier, loginType))
                .thenReturn(Optional.of(auth));

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> {
            authenticationService.findByIdentifierWithLoginType(identifier, loginType);
        });

        assertEquals(UaaErrorResponse.UAA0001.getCode(), exception.getResponse().getCode());
        assertTrue(exception.toString().contains(identifier));
    }

    @ParameterizedTest
    @CsvSource({
            "user@test.com, EMAIL",
            "+85212345678, MOBILE",
            "username123, USERNAME"
    })
    @DisplayName("findByIdentifierWithLoginType should work with all login types")
    void findByIdentifierWithLoginType_shouldWorkWithAllLoginTypes(String identifier, LoginType loginType) {
        // Arrange
        User user = User.builder().id(1L).build();
        Authentication auth = Authentication.builder()
                .identifier(identifier)
                .user(user)
                .build();
        auth.setIsActive(true);

        when(authenticationRepository.findByIdentifierIgnoreCaseAndLoginType(identifier, loginType))
                .thenReturn(Optional.of(auth));

        // Act
        Authentication result = authenticationService.findByIdentifierWithLoginType(identifier, loginType);

        // Assert
        assertNotNull(result);
        assertEquals(identifier, result.getIdentifier());
    }

    // ==================== check Tests ====================

    @Test
    @DisplayName("check should return true when all checks pass")
    void check_shouldReturnTrueWhenAllChecksPass() {
        // Arrange
        String credentials = "password123";
        User user = User.builder()
                .id(1L)
                .status(com.aaax.core.constant.enu.UserStatus.ACTIVE)
                .build();
        user.setIsActive(true);

        Authentication auth = Authentication.builder()
                .identifier("user@test.com")
                .credentials("encodedPassword")
                .user(user)
                .attempts(0)
                .build();

        when(encoder.matches(credentials, "encodedPassword")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        boolean result = authenticationService.check(auth, credentials);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("check should return false when password is incorrect")
    void check_shouldReturnFalseWhenPasswordIncorrect() {
        // Arrange
        String credentials = "wrongPassword";
        User user = User.builder()
                .id(1L)
                .status(com.aaax.core.constant.enu.UserStatus.ACTIVE)
                .build();
        Authentication auth = Authentication.builder()
                .identifier("user@test.com")
                .credentials("encodedPassword")
                .user(user)
                .attempts(0)
                .build();

        when(encoder.matches(credentials, "encodedPassword")).thenReturn(false);

        // Act
        boolean result = authenticationService.check(auth, credentials);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("check should return false when user status is not ACTIVE")
    void check_shouldReturnFalseWhenUserStatusNotActive() {
        // Arrange
        String credentials = "password123";
        User user = User.builder()
                .id(1L)
                .status(com.aaax.core.constant.enu.UserStatus.INACTIVE)
                .build();
        Authentication auth = Authentication.builder()
                .identifier("user@test.com")
                .credentials("encodedPassword")
                .user(user)
                .attempts(0)
                .build();

        when(encoder.matches(credentials, "encodedPassword")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        boolean result = authenticationService.check(auth, credentials);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("check should return false when user is soft deleted")
    void check_shouldReturnFalseWhenUserSoftDeleted() {
        // Arrange
        String credentials = "password123";
        User user = User.builder()
                .id(1L)
                .status(com.aaax.core.constant.enu.UserStatus.ACTIVE)
                .build();
        user.setIsActive(false);
        Authentication auth = Authentication.builder()
                .identifier("user@test.com")
                .credentials("encodedPassword")
                .user(user)
                .attempts(0)
                .build();
        when(encoder.matches(credentials, "encodedPassword")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        boolean result = authenticationService.check(auth, credentials);

        // Assert
        assertFalse(result);
    }

    // ==================== check_password Tests ====================

    @Test
    @DisplayName("check_password should return true for correct password")
    void checkPassword_shouldReturnTrueForCorrectPassword() {
        // Arrange
        String credentials = "password123";
        Authentication auth = Authentication.builder()
                .identifier("user@test.com")
                .credentials("encodedPassword")
                .build();

        when(encoder.matches(credentials, "encodedPassword")).thenReturn(true);

        // Act
        Boolean result = authenticationService.check_password(auth, credentials);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("check_password should return false for incorrect password")
    void checkPassword_shouldReturnFalseForIncorrectPassword() {
        // Arrange
        String credentials = "wrongPassword";
        Authentication auth = Authentication.builder()
                .identifier("user@test.com")
                .credentials("encodedPassword")
                .build();

        when(encoder.matches(credentials, "encodedPassword")).thenReturn(false);

        // Act
        Boolean result = authenticationService.check_password(auth, credentials);

        // Assert
        assertFalse(result);
    }

    // ==================== post_check Tests ====================

    @Test
    @DisplayName("post_check should send kafka event on success")
    void postCheck_shouldSendKafkaEventOnSuccess() {
        // Arrange
        User user = User.builder().id(1L).username("user@test.com").build();
        Authentication auth = Authentication.builder()
                .identifier("user@test.com")
                .user(user)
                .build();

        // Act
        authenticationService.post_check(auth, true);

        // Assert
        verify(kafkaUtil).send(eq(com.aaax.core.kafka.enu.KafkaTopic.USER_LOGIN_ATTEMPTS_MUTATED), any());
    }

    @Test
    @DisplayName("post_check should send kafka event on failure")
    void postCheck_shouldSendKafkaEventOnFailure() {
        // Arrange
        User user = User.builder().id(1L).username("user@test.com").build();
        Authentication auth = Authentication.builder()
                .identifier("user@test.com")
                .user(user)
                .build();

        // Act
        authenticationService.post_check(auth, false);

        // Assert
        verify(kafkaUtil).send(eq(com.aaax.core.kafka.enu.KafkaTopic.USER_LOGIN_ATTEMPTS_MUTATED), any());
    }

    @Test
    @DisplayName("findValidRecordsByDynamicIdentifier should return active authentication")
    void findValidRecordsByDynamicIdentifier_shouldReturnActive() {
        Authentication auth = Authentication.builder().identifier("user@test.com").build();
        auth.setIsActive(true);
        try (MockedStatic<UaaValidation> mockedValidation = mockStatic(UaaValidation.class)) {
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifier("user@test.com")).thenReturn("user@test.com");
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifierIfPresent("user@test.com")).thenReturn("user@test.com");
            mockedValidation.when(() -> UaaValidation.detechLoginType("user@test.com")).thenReturn(LoginType.EMAIL);
            when(authenticationRepository.findByIdentifierIgnoreCaseAndLoginType("user@test.com", LoginType.EMAIL))
                    .thenReturn(Optional.of(auth));

            assertEquals(auth, authenticationService.findValidRecordsByDynamicIdentifier("user@test.com"));
        }
    }

    @Test
    @DisplayName("findValidRecordsByDynamicIdentifier should throw when inactive")
    void findValidRecordsByDynamicIdentifier_shouldThrowWhenInactive() {
        Authentication auth = Authentication.builder().identifier("user@test.com").build();
        auth.setIsActive(false);
        try (MockedStatic<UaaValidation> mockedValidation = mockStatic(UaaValidation.class)) {
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifier("user@test.com")).thenReturn("user@test.com");
            mockedValidation.when(() -> UaaValidation.toCanonicalIdentifierIfPresent("user@test.com")).thenReturn("user@test.com");
            mockedValidation.when(() -> UaaValidation.detechLoginType("user@test.com")).thenReturn(LoginType.EMAIL);
            when(authenticationRepository.findByIdentifierIgnoreCaseAndLoginType("user@test.com", LoginType.EMAIL))
                    .thenReturn(Optional.of(auth));

            assertThrows(BizException.class,
                    () -> authenticationService.findValidRecordsByDynamicIdentifier("user@test.com"));
        }
    }

    @Test
    @DisplayName("decrypt should delegate to CryptographyUtil")
    void decrypt_shouldDelegate() throws Exception {
        PrivateKey privateKey = mock(PrivateKey.class);
        when(rsaKey.toPrivateKey()).thenReturn(privateKey);
        try (MockedStatic<CryptographyUtil> mocked = mockStatic(CryptographyUtil.class)) {
            mocked.when(() -> CryptographyUtil.decrypt(eq("cipher"), eq(privateKey))).thenReturn("plain");

            assertEquals("plain", authenticationService.decrypt(" cipher "));
            mocked.verify(() -> CryptographyUtil.decrypt("cipher", privateKey));
        }
    }

    @Test
    @DisplayName("isThisUsernameExistedForPublicRegister should no-op when username free")
    void isThisUsernameExistedForPublicRegister_shouldNoOpWhenFree() {
        when(authenticationRepository.findAllByIdentifierIgnoreCase("new@test.com")).thenReturn(List.of());

        assertDoesNotThrow(() -> authenticationService.isThisUsernameExistedForPublicRegister("new@test.com"));
    }

    @Test
    @DisplayName("isThisUsernameExistedForPublicRegister should throw 409 for active user")
    void isThisUsernameExistedForPublicRegister_shouldThrow409ForActive() {
        User user = User.builder().id(1L).status(UserStatus.ACTIVE).build();
        Authentication auth = Authentication.builder()
                .identifier("user@test.com")
                .loginType(LoginType.EMAIL)
                .user(user)
                .build();
        auth.setIsActive(true);
        when(authenticationRepository.findAllByIdentifierIgnoreCase("user@test.com")).thenReturn(List.of(auth));

        BizException ex = assertThrows(BizException.class,
                () -> authenticationService.isThisUsernameExistedForPublicRegister("user@test.com"));
        assertEquals(UaaErrorResponse.UAA0409.getCode(), ex.getResponse().getCode());
    }

    @Test
    @DisplayName("isThisUsernameExistedForPublicRegister should throw when inactive auth already verified")
    void isThisUsernameExistedForPublicRegister_shouldThrowWhenVerifiedInactive() {
        User user = User.builder().id(1L).status(UserStatus.PENDING_VERIFY).build();
        Authentication auth = Authentication.builder()
                .identifier("user@test.com")
                .loginType(LoginType.EMAIL)
                .user(user)
                .build();
        auth.setIsActive(false);
        when(authenticationRepository.findAllByIdentifierIgnoreCase("user@test.com")).thenReturn(List.of(auth));
        when(forgotPasswordOtpUseCase.isVerifiedAlready("user@test.com")).thenReturn(true);

        BizException ex = assertThrows(BizException.class,
                () -> authenticationService.isThisUsernameExistedForPublicRegister("user@test.com"));
        assertEquals(UaaErrorResponse.UAA8420.getCode(), ex.getResponse().getCode());
    }

    @Test
    @DisplayName("isThisUsernameExistedForPublicRegister should throw otp metadata when inactive unverified")
    void isThisUsernameExistedForPublicRegister_shouldThrowOtpMetadataWhenUnverified() {
        User user = User.builder().id(1L).status(UserStatus.PENDING_VERIFY).build();
        Authentication auth = Authentication.builder()
                .identifier("user@test.com")
                .loginType(LoginType.EMAIL)
                .user(user)
                .build();
        auth.setIsActive(false);
        when(authenticationRepository.findAllByIdentifierIgnoreCase("user@test.com")).thenReturn(List.of(auth));
        when(forgotPasswordOtpUseCase.isVerifiedAlready("user@test.com")).thenReturn(false);
        OtpMetadata metadata = OtpMetadata.builder().code("123456").ttl(60).build();
        when(forgotPasswordOtpUseCase.queryBackTheStoredValueInRedis("user@test.com")).thenReturn(metadata);

        BizException ex = assertThrows(BizException.class,
                () -> authenticationService.isThisUsernameExistedForPublicRegister("user@test.com"));
        assertEquals(UaaErrorResponse.UAA8400.getCode(), ex.getResponse().getCode());
    }
}
