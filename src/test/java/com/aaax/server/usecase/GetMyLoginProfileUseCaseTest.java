package com.aaax.server.usecase;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.aaax.server.entity.dto.response.GetUserPermissionResponseDto;
import com.aaax.server.entity.dto.response.GetUserRouteResponseDto;
import com.aaax.server.entity.po.UserRoute;
import com.aaax.server.entity.po.user_management.UserPermission;
import com.aaax.server.repository.UserPermissionRepository;
import com.aaax.server.repository.UserRouteRepository;
import com.aaax.server.service.AaaxService;
import com.aaax.core.utils.ResourcesUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetMyLoginProfileUseCaseTest {

    @Mock
    private AaaxService aaaxService;
    @Mock
    private UserRouteRepository userRouteRepository;
    @Mock
    private UserPermissionRepository userPermissionRepository;
    @Mock
    private ResourceLoader resourceLoader;

    @InjectMocks
    private GetMyLoginProfileUseCase getMyLoginProfileUseCase;

    @Test
    @DisplayName("getMyRoutes should return local routes without tenant mesh")
    void getMyRoutes_shouldReturnLocal() {
        UserRoute route =
                UserRoute.builder().id(1L).userId(9L).tenantRoleRouteId(0L).actualRoutes(Map.of("home", "/")).build();
        when(userRouteRepository.findAllByUserId(9L)).thenReturn(List.of(route));

        List<GetUserRouteResponseDto> result = getMyLoginProfileUseCase.getMyRoutes(9L);

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getId());
    }

    @Test
    @DisplayName("getMyRoutes should return empty when none")
    void getMyRoutes_empty() {
        when(userRouteRepository.findAllByUserId(9L)).thenReturn(List.of());
        assertTrue(getMyLoginProfileUseCase.getMyRoutes(9L).isEmpty());
    }

    @Test
    @DisplayName("getMyPermissions should create default permission when missing")
    void getMyPermissions_shouldCreateDefault() {
        when(userPermissionRepository.findByUserId(3L)).thenReturn(Optional.empty());
        when(userPermissionRepository.save(any())).thenAnswer(inv -> {
            UserPermission up = inv.getArgument(0);
            up.setId(7L);
            return up;
        });
        try (MockedStatic<ResourcesUtil> resources = mockStatic(ResourcesUtil.class)) {
            resources.when(() -> ResourcesUtil.readJson(anyString(), any(), eq(Map.class)))
                    .thenReturn(Map.of("normal", Map.of()));

            GetUserPermissionResponseDto result = getMyLoginProfileUseCase.getMyPermissions(3L);

            assertEquals("upm_7", result.getId());
            assertTrue(result.getRoles().contains("normal"));
        }
    }

    @Test
    @DisplayName("getMyPermissions should reuse existing permission")
    void getMyPermissions_shouldReuseExisting() {
        UserPermission existing = UserPermission.builder()
                .id(8L)
                .userId(3L)
                .apiVersion("1.0")
                .actualPermissions(Map.of("admin", Map.of()))
                .build();
        when(userPermissionRepository.findByUserId(3L)).thenReturn(Optional.of(existing));
        when(userPermissionRepository.save(existing)).thenReturn(existing);
        try (MockedStatic<ResourcesUtil> resources = mockStatic(ResourcesUtil.class)) {
            resources.when(() -> ResourcesUtil.readJson(anyString(), any(), eq(Map.class))).thenReturn(Map.of());

            GetUserPermissionResponseDto result = getMyLoginProfileUseCase.getMyPermissions(3L);
            assertEquals("upm_8", result.getId());
        }
    }
}
