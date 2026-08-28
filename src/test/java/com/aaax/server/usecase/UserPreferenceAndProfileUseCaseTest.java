package com.aaax.server.usecase;

import com.aaax.core.api.UtilApiClient;
import com.aaax.core.entity.dto.aaax.response.GetUserPreferenceResponseDto;
import com.aaax.core.entity.dto.aaax.response.GetUserProfileResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.utils.ResourcesUtil;
import com.aaax.server.entity.dto.request.UpdateUserPreferenceRequestDto;
import com.aaax.server.entity.dto.request.UpdateUserProfileRequestDto;
import com.aaax.server.entity.enu.UserPreferenceType;
import com.aaax.server.entity.enu.UserProfileType;
import com.aaax.server.entity.po.user_management.UserPreference;
import com.aaax.server.entity.po.user_management.UserProfile;
import com.aaax.server.repository.UserPreferenceRepository;
import com.aaax.server.repository.UserProfileRepository;
import com.aaax.server.service.AaaxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceAndProfileUseCaseTest {

    @Mock private UserPreferenceRepository userPreferenceRepository;
    @Mock private AaaxService aaaxService;
    @Mock private ResourceLoader resourceLoader;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UtilApiClient utilApiClient;

    @InjectMocks
    private UserPreferenceUseCase userPreferenceUseCase;

    @InjectMocks
    private UserProfileUseCase userProfileUseCase;

    @Test
    @DisplayName("getUserPreference should throw for unknown key")
    void getUserPreference_shouldThrowForUnknownKey() {
        try (MockedStatic<ResourcesUtil> resources = mockStatic(ResourcesUtil.class)) {
            resources.when(() -> ResourcesUtil.readJson(anyString(), any(), eq(Map.class)))
                    .thenReturn(Map.of("general", Map.of()));
            assertThrows(BizException.class, () -> userPreferenceUseCase.getUserPreference("u_1", "unknown"));
        }
    }

    @Test
    @DisplayName("getUserPreference should return existing preference")
    void getUserPreference_shouldReturnExisting() {
        UserPreference preference = UserPreference.builder()
                .id(1L).userId(1L).key("general")
                .actualPreference(Map.of("themes", Map.of("selected", "dark")))
                .build();
        preference.setUpdateDt(java.time.Instant.now());
        when(aaaxService.getById(1L)).thenReturn(null);
        when(userPreferenceRepository.findByUserIdAndTypeAndKey(1L, UserPreferenceType.DEFAULT.name(), "general"))
                .thenReturn(Optional.of(preference));
        try (MockedStatic<ResourcesUtil> resources = mockStatic(ResourcesUtil.class)) {
            resources.when(() -> ResourcesUtil.readJson(anyString(), any(), eq(Map.class)))
                    .thenReturn(Map.of("general", Map.of("themes", Map.of())));

            GetUserPreferenceResponseDto result = userPreferenceUseCase.getUserPreference("u_1", "general");
            assertNotNull(result.getContext());
        }
    }

    @Test
    @DisplayName("generateUserPreference should persist default preference")
    void generateUserPreference_shouldPersist() {
        when(userPreferenceRepository.save(any())).thenAnswer(inv -> {
            UserPreference up = inv.getArgument(0);
            up.setId(5L);
            return up;
        });
        UserPreference result = userPreferenceUseCase.generateUserPreference("u_1", "general", Map.of("a", 1));
        assertEquals(5L, result.getId());
        assertEquals("general", result.getKey());
    }

    @Test
    @DisplayName("queryUserPreference should map all preferences")
    void queryUserPreference_shouldMapAll() {
        when(aaaxService.getById("1")).thenReturn(null);
        UserPreference preference = UserPreference.builder()
                .id(1L).userId(1L).actualPreference(Map.of("x", 1)).build();
        preference.setUpdateDt(java.time.Instant.now());
        when(userPreferenceRepository.findAllByUserIdAndType(1L, UserPreferenceType.DEFAULT.name()))
                .thenReturn(List.of(preference));

        List<GetUserPreferenceResponseDto> result = userPreferenceUseCase.queryUserPreference("u_1");
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("updateUserPreference(dto) should return existing preference")
    void updateUserPreferenceDto_shouldReturnExisting() {
        UserPreference preference = UserPreference.builder()
                .id(1L).userId(1L).actualPreference(new HashMap<>()).build();
        preference.setUpdateDt(java.time.Instant.now());
        when(userPreferenceRepository.findByUserIdAndTypeAndKey(1L, UserPreferenceType.DEFAULT.name(), "general"))
                .thenReturn(Optional.of(preference));

        GetUserPreferenceResponseDto result = userPreferenceUseCase.updateUserPreference(
                "u_1", "general", UpdateUserPreferenceRequestDto.builder().build());
        assertNotNull(result);
    }

    @Test
    @DisplayName("getOneProfile should return profile by alias")
    void getOneProfile_shouldReturnByAlias() {
        UserProfile profile = UserProfile.builder().id(2L).userId(3L).alias("nick").build();
        when(userProfileRepository.findByAlias("nick")).thenReturn(Optional.of(profile));

        GetUserProfileResponseDto result = userProfileUseCase.getOneProfile("nick");
        assertEquals("nick", result.getAlias());
    }

    @Test
    @DisplayName("getOneProfile should throw when missing")
    void getOneProfile_shouldThrowWhenMissing() {
        when(userProfileRepository.findByAlias("missing")).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> userProfileUseCase.getOneProfile("missing"));
    }

    @Test
    @DisplayName("getUserProfile should return existing profile")
    void getUserProfile_shouldReturnExisting() {
        UserProfile profile = UserProfile.builder().id(2L).userId(3L).alias("nick").build();
        when(userProfileRepository.findByUserIdAndType(3L, UserProfileType.DEFAULT.name()))
                .thenReturn(Optional.of(profile));

        GetUserProfileResponseDto result = userProfileUseCase.getUserProfile("u_3");
        assertEquals("up_2", result.getId());
    }

    @Test
    @DisplayName("getUserProfile with aspects should return profile")
    void getUserProfile_withAspects_shouldReturn() {
        UserProfile profile = UserProfile.builder().id(2L).userId(3L).alias("nick").build();
        when(userProfileRepository.findByUserIdAndType(3L, UserProfileType.DEFAULT.name()))
                .thenReturn(Optional.of(profile));

        assertEquals("nick", userProfileUseCase.getUserProfile("u_3", List.of()).getAlias());
    }

    @Test
    @DisplayName("updateUserPreference should validate and update selected theme")
    void updateUserPreference_shouldUpdateTheme() {
        Map<String, Object> generalConfig = new HashMap<>();
        generalConfig.put("themes", new HashMap<>(Map.of(
                "options", List.of("DARK", "LIGHT", "SYSTEM"),
                "selected", "LIGHT"
        )));
        Map keys = Map.of("general", generalConfig);
        Map validations = Map.of(
                "themes", Map.of("isRecursive", false, "values", List.of("selected"))
        );

        UserPreference preference = UserPreference.builder()
                .id(1L).userId(1L).key("general")
                .actualPreference(new HashMap<>(generalConfig))
                .build();
        preference.setUpdateDt(java.time.Instant.now());
        when(userPreferenceRepository.findByUserIdAndTypeAndKey(1L, UserPreferenceType.DEFAULT.name(), "general"))
                .thenReturn(Optional.of(preference));
        when(userPreferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<ResourcesUtil> resources = mockStatic(ResourcesUtil.class)) {
            resources.when(() -> ResourcesUtil.readJson(contains("user_preference_keys"), any(), eq(Map.class)))
                    .thenReturn(keys);
            resources.when(() -> ResourcesUtil.readJson(contains("user_preference_validations"), any(), eq(Map.class)))
                    .thenReturn(validations);

            GetUserPreferenceResponseDto result = userPreferenceUseCase.updateUserPreference(
                    "1", "general", "themes", Map.of("selected", "DARK"));

            assertNotNull(result);
            verify(userPreferenceRepository).save(any());
        }
    }

    @Test
    @DisplayName("updateUserPreference should reject invalid selected option")
    void updateUserPreference_shouldRejectInvalidOption() {
        Map<String, Object> themes = new HashMap<>(Map.of(
                "options", List.of("DARK", "LIGHT"),
                "selected", "LIGHT"
        ));
        Map keys = Map.of("general", Map.of("themes", themes));
        Map validations = Map.of(
                "themes", Map.of("isRecursive", false, "values", List.of("selected"))
        );

        try (MockedStatic<ResourcesUtil> resources = mockStatic(ResourcesUtil.class)) {
            resources.when(() -> ResourcesUtil.readJson(contains("user_preference_keys"), any(), eq(Map.class)))
                    .thenReturn(keys);
            resources.when(() -> ResourcesUtil.readJson(contains("user_preference_validations"), any(), eq(Map.class)))
                    .thenReturn(validations);

            assertThrows(BizException.class, () ->
                    userPreferenceUseCase.updateUserPreference("1", "general", "themes", Map.of("selected", "NEON")));
        }
    }

    @Test
    @DisplayName("doCreateDefault should persist new profile metadata")
    void doCreateDefault_shouldPersist() {
        when(userProfileRepository.findByUserIdAndType(8L, UserProfileType.DEFAULT.name()))
                .thenReturn(Optional.empty());
        when(aaaxService.getById(8L)).thenReturn(
                com.aaax.server.entity.po.user.User.builder().id(8L).username("user@test.com").build());
        when(userProfileRepository.save(any())).thenAnswer(inv -> {
            UserProfile p = inv.getArgument(0);
            p.setId(88L);
            return p;
        });

        try (MockedStatic<ResourcesUtil> resources = mockStatic(ResourcesUtil.class)) {
            resources.when(() -> ResourcesUtil.readJson(anyString(), any(), eq(Map.class)))
                    .thenReturn(Map.of("idvStatus", "PENDING"));

            UserProfile created = userProfileUseCase.doCreateDefault(new HashMap<>(), 8L);

            assertEquals(88L, created.getId());
            assertEquals("user@test.com", created.getAlias());
            assertNotNull(((Map<?, ?>) created.getContext()).get("email"));
        }
    }

    @Test
    @DisplayName("doCreateDefault should return existing profile")
    void doCreateDefault_shouldReturnExisting() {
        UserProfile existing = UserProfile.builder().id(3L).userId(8L).alias("a").build();
        when(userProfileRepository.findByUserIdAndType(8L, UserProfileType.DEFAULT.name()))
                .thenReturn(Optional.of(existing));
        assertEquals(3L, userProfileUseCase.doCreateDefault(Map.of(), 8L).getId());
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserProfile should save nested context updates")
    void updateUserProfile_shouldSave() {
        UserProfile profile = UserProfile.builder()
                .id(2L).userId(3L).alias("nick")
                .context(new HashMap<>(Map.of("firstName", "Old", "verification", Map.of("idvStatus", "NA"))))
                .build();
        when(userProfileRepository.findByUserIdAndType(3L, UserProfileType.DEFAULT.name()))
                .thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GetUserProfileResponseDto result = userProfileUseCase.updateUserProfile(
                "3",
                UpdateUserProfileRequestDto.builder().context(new HashMap<>(Map.of("firstName", "New"))).build(),
                "nick@test.com",
                "QS");

        assertNotNull(result);
        verify(userProfileRepository).save(any());
    }

    @Test
    @DisplayName("updateUserProfileMgt should resolve username then update")
    void updateUserProfileMgt_shouldDelegate() {
        when(aaaxService.getById("3")).thenReturn(
                com.aaax.server.entity.po.user.User.builder().id(3L).username("u@test.com").build());
        UserProfile profile = UserProfile.builder()
                .id(2L).userId(3L).alias("nick")
                .context(new HashMap<>(Map.of("verification", Map.of("idvStatus", "NA"))))
                .build();
        when(userProfileRepository.findByUserIdAndType(3L, UserProfileType.DEFAULT.name()))
                .thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertNotNull(userProfileUseCase.updateUserProfileMgt(
                "3",
                UpdateUserProfileRequestDto.builder().context(new HashMap<>(Map.of("lastName", "X"))).build(),
                "QS"));
    }
}
