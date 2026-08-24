package com.aaax.endpoint.api.my_login_profile;

import com.aaax.core.response.Result;
import com.aaax.core.utils.JwtUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.entity.dto.response.GetUserPermissionResponseDto;
import com.aaax.entity.dto.response.GetUserRouteResponseDto;
import com.aaax.usecase.GetMyLoginProfileUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyLoginProfileEndpointTest {

    @Mock private GetMyLoginProfileUseCase getMyLoginProfileUseCase;
    @Mock private RedisUtil redisUtil;

    @InjectMocks
    private MyLoginProfileEndpoint endpoint;

    @Test
    @DisplayName("getMyRoutes should load from use case on redis miss")
    void getMyRoutes_shouldLoadOnMiss() {
        when(redisUtil.get(anyString())).thenReturn(null);
        when(getMyLoginProfileUseCase.getMyRoutes(10L))
                .thenReturn(List.of(GetUserRouteResponseDto.builder().id("ur_1").build()));

        try (MockedStatic<JwtUtil> jwt = mockStatic(JwtUtil.class)) {
            jwt.when(JwtUtil::userId).thenReturn("10");
            Result<List<GetUserRouteResponseDto>> result = endpoint.getMyRoutes();
            assertEquals(1, result.getData().size());
            verify(redisUtil).set(contains("10"), any(), eq(30L));
        }
    }

    @Test
    @DisplayName("getMyRoutes should return redis cache hit")
    void getMyRoutes_shouldReturnCacheHit() {
        when(redisUtil.get(anyString()))
                .thenReturn(List.of(GetUserRouteResponseDto.builder().id("ur_cached").build()));

        try (MockedStatic<JwtUtil> jwt = mockStatic(JwtUtil.class)) {
            jwt.when(JwtUtil::userId).thenReturn("10");
            Result<List<GetUserRouteResponseDto>> result = endpoint.getMyRoutes();
            assertEquals("ur_cached", result.getData().get(0).getId());
            verify(getMyLoginProfileUseCase, never()).getMyRoutes(anyLong());
            verify(redisUtil, never()).hasKey(anyString());
        }
    }

    @Test
    @DisplayName("getMyPermissions should load on redis miss")
    void getMyPermissions_shouldLoadOnMiss() {
        when(redisUtil.get(anyString())).thenReturn(null);
        when(getMyLoginProfileUseCase.getMyPermissions(10L))
                .thenReturn(GetUserPermissionResponseDto.builder().id("up_1").build());

        try (MockedStatic<JwtUtil> jwt = mockStatic(JwtUtil.class)) {
            jwt.when(JwtUtil::userId).thenReturn("10");
            assertEquals("up_1", endpoint.getMyPermissions().getData().getId());
        }
    }

    @Test
    @DisplayName("getUserRouteOfThisUser should load for path userId")
    void getUserRouteOfThisUser_shouldLoad() {
        when(redisUtil.get(anyString())).thenReturn(null);
        when(getMyLoginProfileUseCase.getMyRoutes(7L)).thenReturn(List.of());
        assertNotNull(endpoint.getUserRouteOfThisUser("u_7"));
        verify(getMyLoginProfileUseCase).getMyRoutes(7L);
    }

    @Test
    @DisplayName("getUserPermissionsOfThisUser should load on redis miss")
    void getUserPermissionsOfThisUser_shouldLoad() {
        when(redisUtil.get(anyString())).thenReturn(null);
        when(getMyLoginProfileUseCase.getMyPermissions(7L))
                .thenReturn(GetUserPermissionResponseDto.builder().id("up_7").build());
        assertEquals("up_7", endpoint.getUserPermissionsOfThisUser("u_7").getData().getId());
        verify(redisUtil).set(contains("7"), any(), eq(30L));
    }

    @Test
    @DisplayName("getMyPermissions should return redis cache hit")
    void getMyPermissions_shouldReturnCacheHit() {
        when(redisUtil.get(anyString()))
                .thenReturn(GetUserPermissionResponseDto.builder().id("cached").build());
        try (MockedStatic<JwtUtil> jwt = mockStatic(JwtUtil.class)) {
            jwt.when(JwtUtil::userId).thenReturn("10");
            assertEquals("cached", endpoint.getMyPermissions().getData().getId());
            verify(getMyLoginProfileUseCase, never()).getMyPermissions(anyLong());
        }
    }

    @Test
    @DisplayName("getUserRouteOfThisUser should return redis cache hit")
    void getUserRouteOfThisUser_shouldReturnCacheHit() {
        when(redisUtil.get(anyString()))
                .thenReturn(List.of(GetUserRouteResponseDto.builder().id("ur_c").build()));
        assertEquals("ur_c", endpoint.getUserRouteOfThisUser("u_7").getData().get(0).getId());
    }
}
