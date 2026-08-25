package com.aaax.server.endpoint.api.user_profile;

import com.aaax.core.entity.dto.uaa.response.GetUserProfileResponseDto;
import com.aaax.core.response.Result;
import com.aaax.core.utils.JwtUtil;
import com.aaax.server.entity.dto.request.UpdateUserProfileRequestDto;
import com.aaax.server.usecase.UserProfileUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileEndpointTest {

    @Mock private UserProfileUseCase userProfileUseCase;

    @InjectMocks
    private UserProfileEndpoint endpoint;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(endpoint, "systemInvoker", "QS");
    }

    @Test
    @DisplayName("getMyProfiles should resolve jwt userId")
    void getMyProfiles_shouldDelegate() {
        when(userProfileUseCase.getUserProfile("10", List.of()))
                .thenReturn(GetUserProfileResponseDto.builder().id("up_1").build());
        try (MockedStatic<JwtUtil> jwt = mockStatic(JwtUtil.class)) {
            jwt.when(JwtUtil::userId).thenReturn("10");
            Result<GetUserProfileResponseDto> result = endpoint.getMyProfiles(null);
            assertEquals("up_1", result.getData().getId());
        }
    }

    @Test
    @DisplayName("getProfile should lookup by alias")
    void getProfile_shouldDelegate() {
        when(userProfileUseCase.getOneProfile("alias"))
                .thenReturn(GetUserProfileResponseDto.builder().id("up_2").build());
        assertEquals("up_2", endpoint.getProfile("alias").getData().getId());
    }

    @Test
    @DisplayName("updateMyProfile should use system invoker when ss absent")
    void updateMyProfile_shouldDelegate() {
        UpdateUserProfileRequestDto request = UpdateUserProfileRequestDto.builder()
                .context(Map.of("firstName", "A"))
                .build();
        when(userProfileUseCase.updateUserProfile(eq("10"), eq(request), eq("u@test.com"), eq("QS")))
                .thenReturn(GetUserProfileResponseDto.builder().id("up_3").build());
        try (MockedStatic<JwtUtil> jwt = mockStatic(JwtUtil.class)) {
            jwt.when(JwtUtil::userId).thenReturn("10");
            jwt.when(() -> JwtUtil.getFromJwt(JwtUtil.METADATA)).thenReturn(Map.of("identifier", "u@test.com"));
            assertEquals("up_3", endpoint.updateMyProfile(request, null).getData().getId());
        }
    }

    @Test
    @DisplayName("updateMyProfileWithIcon should pass ss override")
    void updateMyProfileWithIcon_shouldDelegate() {
        UpdateUserProfileRequestDto request = UpdateUserProfileRequestDto.builder().context(Map.of()).build();
        when(userProfileUseCase.updateUserProfile(eq("10"), eq(request), eq("u@test.com"), eq("WEB")))
                .thenReturn(GetUserProfileResponseDto.builder().id("up_4").build());
        try (MockedStatic<JwtUtil> jwt = mockStatic(JwtUtil.class)) {
            jwt.when(JwtUtil::userId).thenReturn("10");
            jwt.when(() -> JwtUtil.getFromJwt(JwtUtil.METADATA)).thenReturn(Map.of("identifier", "u@test.com"));
            assertEquals("up_4", endpoint.updateMyProfileWithIcon(request, "WEB").getData().getId());
        }
    }
}
