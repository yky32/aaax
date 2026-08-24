package com.aaax.usecase.user_engagement;

import com.aaax.core.common.PushSettingDto;
import com.aaax.core.common.jsonfield.DeviceMetadata;
import com.aaax.core.entity.dto.uaa.response.GetUserDeviceResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserPreferenceResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserProfileResponseDto;
import com.aaax.usecase.UserDeviceUseCase;
import com.aaax.usecase.UserPreferenceUseCase;
import com.aaax.usecase.UserProfileUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryPushDataUseCaseTest {

    @Mock private UserDeviceUseCase userDeviceUseCase;
    @Mock private UserProfileUseCase userProfileUseCase;
    @Mock private UserPreferenceUseCase userPreferenceUseCase;

    @InjectMocks
    private QueryPushDataUseCase queryPushDataUseCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(queryPushDataUseCase, "systemInvoker", "APP");
    }

    @Test
    @DisplayName("execute should return NA fcm when no devices")
    void execute_shouldHandleEmptyDevices() {
        when(userDeviceUseCase.myDevices("1", "APP")).thenReturn(List.of());
        when(userProfileUseCase.getUserProfile("1"))
                .thenReturn(GetUserProfileResponseDto.builder().context(Map.of("phone", "852-1")).build());
        when(userPreferenceUseCase.getUserPreference("1", "general"))
                .thenReturn(GetUserPreferenceResponseDto.builder()
                        .context(Map.of(
                                "localizations", Map.of("selected", "zh"),
                                "themes", Map.of("selected", "DARK"),
                                "notifications", Map.of("email", Map.of("enabled", true))
                        ))
                        .build());

        PushSettingDto result = queryPushDataUseCase.execute(1L);

        assertEquals("NA-NOT-FOUND", result.getFcmToken());
        assertEquals("852-1", result.getPhone());
        assertEquals("u_1", result.getUserId());
        assertEquals("zh", result.getLocale());
        assertEquals("DARK", result.getTheme());
    }

    @Test
    @DisplayName("execute should extract fcm token from first device")
    void execute_shouldExtractFcm() {
        DeviceMetadata device = DeviceMetadata.builder()
                .token(Map.of("fcm", "token-abc"))
                .build();
        when(userDeviceUseCase.myDevices("2", "APP")).thenReturn(List.of(
                GetUserDeviceResponseDto.builder().context(List.of(device)).build()));
        when(userProfileUseCase.getUserProfile("2"))
                .thenReturn(GetUserProfileResponseDto.builder().context(Map.of()).build());
        when(userPreferenceUseCase.getUserPreference("2", "general"))
                .thenReturn(GetUserPreferenceResponseDto.builder().context(Map.of()).build());

        PushSettingDto result = queryPushDataUseCase.execute(2L);

        assertEquals("token-abc", result.getFcmToken());
        assertEquals("en", result.getLocale());
        assertEquals("SYSTEM", result.getTheme());
    }
}
