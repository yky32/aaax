package com.aaax.server.usecase;

import com.aaax.core.entity.dto.aaax.response.*;
import com.aaax.server.service.AaaxService;
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
class UserMetricsUseCaseTest {

    @Mock private AaaxService aaaxService;
    @Mock private UserPreferenceUseCase userPreferenceUseCase;
    @Mock private UserDeviceUseCase userDeviceUseCase;
    @Mock private UserProfileUseCase userProfileUseCase;
    @Mock private UserIdentityVerificationUseCase userIdentityVerificationUseCase;

    @InjectMocks
    private UserMetricsUseCase userMetricsUseCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userMetricsUseCase, "userIdentityVerificationUseCase", userIdentityVerificationUseCase);
    }

    @Test
    @DisplayName("execute should aggregate preference profile device and verifications")
    void execute_shouldAggregate() {
        when(aaaxService.get(1L)).thenReturn(GetUserResponseDto.builder().id("u_1").build());
        when(userPreferenceUseCase.getUserPreference("u_1", "general"))
                .thenReturn(GetUserPreferenceResponseDto.builder().context(Map.of("theme", "dark")).build());
        when(userDeviceUseCase.myDevicesOfSourceSystem("u_1", "APP"))
                .thenReturn(GetUserDeviceResponseDto.builder().id("ud_1").build());
        when(userProfileUseCase.getUserProfile("u_1"))
                .thenReturn(GetUserProfileResponseDto.builder().alias("nick").build());
        when(userIdentityVerificationUseCase.myVerifications("u_1"))
                .thenReturn(List.of(GetUserVerificationResponseDto.builder().id("uv_1").build()));

        GetUserMetricsResponseDto result = userMetricsUseCase.execute("u_1", "APP");

        assertEquals("u_1", result.getUser().getId());
        assertNotNull(result.getPreference());
        assertNotNull(result.getDevice());
        assertEquals("nick", result.getProfile().getAlias());
        assertEquals(1, result.getVerifications().size());
    }

    @Test
    @DisplayName("execute should skip device when sourceSystem null")
    void execute_shouldSkipDeviceWhenNoSourceSystem() {
        when(aaaxService.get(2L)).thenReturn(GetUserResponseDto.builder().id("u_2").build());
        when(userPreferenceUseCase.getUserPreference("u_2", "general"))
                .thenReturn(GetUserPreferenceResponseDto.builder().build());
        when(userProfileUseCase.getUserProfile("u_2"))
                .thenReturn(GetUserProfileResponseDto.builder().build());
        when(userIdentityVerificationUseCase.myVerifications("u_2")).thenReturn(List.of());

        GetUserMetricsResponseDto result = userMetricsUseCase.execute("u_2", null);

        assertNull(result.getDevice());
        verify(userDeviceUseCase, never()).myDevicesOfSourceSystem(anyString(), anyString());
    }
}
