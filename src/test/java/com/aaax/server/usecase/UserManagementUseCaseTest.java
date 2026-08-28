package com.aaax.server.usecase;

import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.entity.dto.aaax.response.GetUserResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.utils.KafkaUtil;
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
import com.aaax.server.service.AaaxService;
import com.aaax.server.validation.PasswordPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementUseCaseTest {

    @Mock private UserPermissionRepository userPermissionRepository;
    @Mock private UserDeviceRepository userDeviceRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserPreferenceRepository userPreferenceRepository;
    @Mock private UserTokenRepository userTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordPolicy passwordPolicy;
    @Mock private AuthenticationRepository authenticationRepository;
    @Mock private UserRouteRepository userRouteRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserVerificationRepository userVerificationRepository;
    @Mock private AaaxService aaaxService;
    @Mock private AuthenticationService authenticationService;
    @Mock private KafkaUtil kafkaUtil;

    @InjectMocks
    private UserManagementUseCase userManagementUseCase;

    private User buildUser(Long id, String username) {
        User user = User.builder().id(id).username(username).status(UserStatus.ACTIVE).build();
        user.setIsActive(true);
        Authentication auth = Authentication.builder().id(10L).identifier(username).user(user).credentials("old").build();
        auth.setIsActive(true);
        user.setAuthentications(new ArrayList<>(List.of(auth)));
        return user;
    }

    @Test
    @DisplayName("updateCredentials should encode and save password")
    void updateCredentials_shouldEncodeAndSave() {
        User user = buildUser(1L, "user@test.com");
        Authentication auth = user.getAuthentications().get(0);
        when(aaaxService.getByUsername("user@test.com")).thenReturn(auth);
        when(passwordPolicy.encode(passwordEncoder, "NewPass1")).thenReturn("encoded");

        GetUserResponseDto result = userManagementUseCase.updateCredentials(
                UpdatePasswordRequestDto.builder().credentials("NewPass1").build(), "user@test.com");

        assertEquals("u_1", result.getId());
        assertEquals("encoded", auth.getCredentials());
        verify(authenticationRepository).save(auth);
    }

    @Test
    @DisplayName("updateCredentials should throw when password blank")
    void updateCredentials_shouldThrowWhenBlank() {
        User user = buildUser(1L, "user@test.com");
        when(aaaxService.getByUsername("user@test.com")).thenReturn(user.getAuthentications().get(0));
        assertThrows(BizException.class, () -> userManagementUseCase.updateCredentials(
                UpdatePasswordRequestDto.builder().credentials("").build(), "user@test.com"));
    }

    @Test
    @DisplayName("updateUsername should sync user, auth, and UserProfile.context.email")
    void updateUsername_shouldSyncUserAuthAndProfileEmail() {
        User user = buildUser(7L, "old@test.com");
        Authentication auth = user.getAuthentications().get(0);
        auth.setLoginType(LoginType.EMAIL);
        Map<String, Object> context = new HashMap<>();
        context.put("email", "old@test.com");
        context.put("firstName", "Ada");
        UserProfile profile = UserProfile.builder()
                .id(70L)
                .userId(7L)
                .type(UserProfileType.DEFAULT.name())
                .alias("old@test.com")
                .context(context)
                .build();

        when(aaaxService.getByUsername("old@test.com")).thenReturn(auth);
        doNothing().when(authenticationService).isThisUsernameExistedForPublicRegister("new@test.com");
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(authenticationRepository.saveAndFlush(auth)).thenReturn(auth);
        when(userProfileRepository.findByUserIdAndType(7L, UserProfileType.DEFAULT.name()))
                .thenReturn(Optional.of(profile));
        when(userProfileRepository.saveAndFlush(profile)).thenReturn(profile);

        GetUserResponseDto result = userManagementUseCase.updateUsername(
                UpdateUsernameRequestDto.builder().username("New@Test.com").build(),
                "Old@Test.com");

        assertEquals("u_7", result.getId());
        assertEquals("new@test.com", user.getUsername());
        assertEquals("new@test.com", auth.getIdentifier());
        assertEquals(LoginType.EMAIL, auth.getLoginType());
        assertEquals("new@test.com", ((Map<?, ?>) profile.getContext()).get("email"));
        assertEquals("Ada", ((Map<?, ?>) profile.getContext()).get("firstName"));
        assertEquals("new@test.com", profile.getAlias());
        verify(userRepository).saveAndFlush(user);
        verify(authenticationRepository).saveAndFlush(auth);
        verify(userProfileRepository).saveAndFlush(profile);
    }

    @Test
    @DisplayName("updateUsername should not overwrite custom alias")
    void updateUsername_shouldNotOverwriteCustomAlias() {
        User user = buildUser(8L, "old@test.com");
        Authentication auth = user.getAuthentications().get(0);
        auth.setLoginType(LoginType.EMAIL);
        Map<String, Object> context = new HashMap<>();
        context.put("email", "old@test.com");
        UserProfile profile = UserProfile.builder()
                .id(80L)
                .userId(8L)
                .type(UserProfileType.DEFAULT.name())
                .alias("custom-nick")
                .context(context)
                .build();

        when(aaaxService.getByUsername("old@test.com")).thenReturn(auth);
        doNothing().when(authenticationService).isThisUsernameExistedForPublicRegister("new@test.com");
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(authenticationRepository.saveAndFlush(auth)).thenReturn(auth);
        when(userProfileRepository.findByUserIdAndType(8L, UserProfileType.DEFAULT.name()))
                .thenReturn(Optional.of(profile));
        when(userProfileRepository.saveAndFlush(profile)).thenReturn(profile);

        userManagementUseCase.updateUsername(
                UpdateUsernameRequestDto.builder().username("new@test.com").build(),
                "old@test.com");

        assertEquals("new@test.com", ((Map<?, ?>) profile.getContext()).get("email"));
        assertEquals("custom-nick", profile.getAlias());
    }

    @Test
    @DisplayName("updateUsername should no-op profile sync when UserProfile missing")
    void updateUsername_shouldSkipMissingProfile() {
        User user = buildUser(9L, "old@test.com");
        Authentication auth = user.getAuthentications().get(0);
        when(aaaxService.getByUsername("old@test.com")).thenReturn(auth);
        doNothing().when(authenticationService).isThisUsernameExistedForPublicRegister("new@test.com");
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(authenticationRepository.saveAndFlush(auth)).thenReturn(auth);
        when(userProfileRepository.findByUserIdAndType(9L, UserProfileType.DEFAULT.name()))
                .thenReturn(Optional.empty());

        GetUserResponseDto result = userManagementUseCase.updateUsername(
                UpdateUsernameRequestDto.builder().username("new@test.com").build(),
                "old@test.com");

        assertEquals("new@test.com", user.getUsername());
        assertEquals("u_9", result.getId());
        verify(userProfileRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("deleteByUserId soft delete should deactivate user and auth")
    void deleteByUserId_softDelete_shouldDeactivate() {
        User user = buildUser(2L, "user@test.com");
        when(aaaxService.getById("u_2")).thenReturn(user);
        try (MockedStatic<com.aaax.core.utils.JwtUtil> jwt = mockStatic(com.aaax.core.utils.JwtUtil.class)) {
            jwt.when(com.aaax.core.utils.JwtUtil::userId).thenThrow(new RuntimeException("no jwt"));

            userManagementUseCase.deleteByUserId("u_2", true);

            assertFalse(user.getIsActive());
            assertFalse(user.getAuthentications().get(0).getIsActive());
            verify(userRepository).saveAndFlush(user);
        }
    }

    @Test
    @DisplayName("deleteByUserId soft delete should no-op when already inactive")
    void deleteByUserId_softDelete_shouldNoOpWhenInactive() {
        User user = buildUser(2L, "user@test.com");
        user.setIsActive(false);
        when(aaaxService.getById("u_2")).thenReturn(user);

        userManagementUseCase.deleteByUserId("u_2", true);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("deleteByUserId hard delete should remove related records and publish kafka")
    void deleteByUserId_hardDelete_shouldRemove() {
        User user = buildUser(3L, "user@test.com");
        when(aaaxService.getById("u_3")).thenReturn(user);
        when(userRouteRepository.findAllByUserId(3L)).thenReturn(List.of(UserRoute.builder().id(99L).userId(3L).build()));
        try (MockedStatic<com.aaax.core.utils.JwtUtil> jwt = mockStatic(com.aaax.core.utils.JwtUtil.class)) {
            jwt.when(com.aaax.core.utils.JwtUtil::userId).thenThrow(new RuntimeException("no jwt"));

            userManagementUseCase.deleteByUserId("u_3", false);

            verify(userPermissionRepository).deleteByUserId(3L);
            verify(userDeviceRepository).deleteByUserId(3L);
            verify(userProfileRepository).deleteByUserId(3L);
            verify(userPreferenceRepository).deleteByUserId(3L);
            verify(userTokenRepository).deleteByUserId(3L);
            verify(userVerificationRepository).deleteByUserId(3L);
            verify(userRouteRepository).deleteAllById(List.of(99L));
            verify(authenticationRepository).deleteAllById(List.of(10L));
            verify(userRepository).deleteById(3L);
            verify(kafkaUtil).send(anyString(), any());
        }
    }

    @Test
    @DisplayName("delete should reject protected accounts")
    void delete_shouldRejectProtected() {
        User user = buildUser(1L, "admin@aaax.local");
        when(aaaxService.getById("u_1")).thenReturn(user);
        assertThrows(BizException.class, () -> userManagementUseCase.deleteByUserId("u_1", false));
    }

    @Test
    @DisplayName("deleteByIdentifier should soft delete via identifier lookup")
    void deleteByIdentifier_shouldSoftDelete() {
        User user = buildUser(4L, "user@test.com");
        when(aaaxService.getUserFromIdentifier("user@test.com")).thenReturn(user);
        try (MockedStatic<com.aaax.core.utils.JwtUtil> jwt = mockStatic(com.aaax.core.utils.JwtUtil.class)) {
            jwt.when(com.aaax.core.utils.JwtUtil::userId).thenThrow(new RuntimeException("no jwt"));
            userManagementUseCase.deleteByIdentifier("user@test.com", true);
            verify(userRepository).saveAndFlush(user);
        }
    }

    @Test
    @DisplayName("updateStatuses should update status and publish event")
    void updateStatuses_shouldUpdateAndPublish() {
        User user = buildUser(5L, "user@test.com");
        when(aaaxService.getUserFromIdentifier("user@test.com")).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        GetUserResponseDto result = userManagementUseCase.updateStatuses(
                UpdateUserStatusRequestDto.builder().status("SUSPENDED").build(), "user@test.com");

        assertEquals(UserStatus.SUSPENDED, user.getStatus());
        assertEquals("u_5", result.getId());
        verify(kafkaUtil).send(anyString(), any());
    }

    @Test
    @DisplayName("getAllUsers should delegate to aaaxService")
    void getAllUsers_shouldDelegate() {
        when(aaaxService.getAll(any(), isNull(), isNull(), eq("t1"), anyList(), anyList(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(com.aaax.core.response.PaginationDto.builder());
        assertNotNull(userManagementUseCase.getAllUsers(PageRequest.of(0, 10), null, null, "t1", null));
    }

    @Test
    @DisplayName("testingDeleteAll should hard-delete non-admin users")
    void testingDeleteAll_shouldDeleteNonAdmins() {
        User user = buildUser(6L, "user@test.com");
        when(userRepository.findByUsernameNotInIgnoreCase(anyList())).thenReturn(List.of(user));
        when(userRouteRepository.findAllByUserId(6L)).thenReturn(List.of());
        try (MockedStatic<com.aaax.core.utils.JwtUtil> jwt = mockStatic(com.aaax.core.utils.JwtUtil.class)) {
            jwt.when(com.aaax.core.utils.JwtUtil::userId).thenThrow(new RuntimeException("no jwt"));
            assertEquals(1, userManagementUseCase.testingDeleteAll());
            verify(userRepository).deleteById(6L);
        }
    }
}
