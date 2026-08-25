package com.aaax.server.endpoint.api.user_profile;

import com.aaax.core.entity.dto.uaa.response.GetUserProfileResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.utils.JwtUtil;
import com.aaax.server.usecase.UserProfileUseCase;
import com.aaax.server.usecase.user_customization.UpdateAvatarUseCase;
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
class UserCustomizationEndpointTest {

    @Mock private UpdateAvatarUseCase updateAvatarUseCase;
    @Mock private UserProfileUseCase userProfileUseCase;

    @InjectMocks
    private UserCustomizationEndpoint endpoint;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(endpoint, "systemInvoker", "QS");
    }

    @Test
    @DisplayName("updateAvatar files should execute use case")
    void updateAvatar_files_shouldDelegate() {
        when(userProfileUseCase.getUserProfile("10"))
                .thenReturn(GetUserProfileResponseDto.builder().id("up_1").build());
        try (MockedStatic<JwtUtil> jwt = mockStatic(JwtUtil.class)) {
            jwt.when(JwtUtil::userId).thenReturn("10");
            assertEquals("up_1", endpoint.updateAvatar(List.of()).getData().getId());
            verify(updateAvatarUseCase).execute(eq(10L), any());
        }
    }

    @Test
    @DisplayName("updateAvatar url should reject missing url")
    void updateAvatar_url_shouldRejectMissing() {
        try (MockedStatic<JwtUtil> jwt = mockStatic(JwtUtil.class)) {
            jwt.when(JwtUtil::userId).thenReturn("10");
            assertThrows(BizException.class, () -> endpoint.updateAvatar(Map.of()));
        }
    }

    @Test
    @DisplayName("updateAvatar url should execute url-only path")
    void updateAvatar_url_shouldDelegate() {
        when(userProfileUseCase.getUserProfile("10"))
                .thenReturn(GetUserProfileResponseDto.builder().id("up_2").build());
        try (MockedStatic<JwtUtil> jwt = mockStatic(JwtUtil.class)) {
            jwt.when(JwtUtil::userId).thenReturn("10");
            assertEquals("up_2", endpoint.updateAvatar(Map.of("url", "https://cdn/a.png")).getData().getId());
            verify(updateAvatarUseCase).executeUrlOnly(10L, "https://cdn/a.png");
        }
    }
}
