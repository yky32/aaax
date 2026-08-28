package com.aaax.server.usecase;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.aaax.core.entity.dto.aaax.response.GetUserResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.utils.ResourcesUtil;
import com.aaax.server.entity.dto.response.GetUserPermissionResponseDto;
import com.aaax.server.entity.dto.response.GetUserRouteResponseDto;
import com.aaax.server.entity.po.UserRoute;
import com.aaax.server.entity.po.user_management.UserPermission;
import com.aaax.server.exception.response.UserRouteErrorResponse;
import com.aaax.server.repository.UserPermissionRepository;
import com.aaax.server.repository.UserRouteRepository;
import com.aaax.server.service.DtoWrapper;
import com.aaax.server.service.AaaxService;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Login profile helpers. Tenant-service enrichment removed for OSS.
 */
@Component
@RequiredArgsConstructor
public class GetMyLoginProfileUseCase {

    private final AaaxService aaaxService;
    private final UserRouteRepository userRouteRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final ResourceLoader resourceLoader;

    public List<GetUserRouteResponseDto> getMyRoutes(Long userId) {
        List<UserRoute> userRoutes = userRouteRepository.findAllByUserId(userId);
        if (userRoutes.isEmpty()) {
            return List.of();
        }
        // Local only — no tenant-service context merge
        return userRoutes.stream()
                .map(userRoute -> DtoWrapper.getGetUserRouteResponseDto(userRoute, null))
                .toList();
    }

    private GetUserResponseDto getMyMetadata(Long userId) {
        return aaaxService.get(userId);
    }

    public GetUserPermissionResponseDto getMyPermissions(Long userId) {
        Map config = ResourcesUtil.readJson("config/user_permissions_sample.json", resourceLoader, Map.class);
        Optional<UserPermission> isExistedUserPermission = userPermissionRepository.findByUserId(userId);
        UserPermission userPermission = isExistedUserPermission.orElseGet(() ->
                UserPermission.builder()
                        .apiVersion("1.0")
                        .userId(userId)
                        .actualPermissions(config)
                        .build());
        userPermission = userPermissionRepository.save(userPermission);
        return DtoWrapper.getUserPermissionResponseDto(userPermission);
    }
}
