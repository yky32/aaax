package com.aaax.server.usecase;

import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.utils.ResourcesUtil;
import com.aaax.core.utils.RetrofitCallHandler;
import com.aaax.server.entity.dto.response.GetUserPermissionResponseDto;
import com.aaax.server.entity.dto.response.GetUserRouteResponseDto;
import com.aaax.server.entity.po.UserRoute;
import com.aaax.server.entity.po.user_management.UserPermission;
import com.aaax.server.exception.response.UserRouteErrorResponse;
import com.aaax.server.ext.api.client.tenant.TenantApiClient;
import com.aaax.server.repository.UserPermissionRepository;
import com.aaax.server.repository.UserRouteRepository;
import com.aaax.server.service.DtoWrapper;
import com.aaax.server.service.UaaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetMyLoginProfileUseCase {

    private final UaaService uaaService;
    private final UserRouteRepository userRouteRepository;
    private final TenantApiClient tenantApiClient;
    private final UserPermissionRepository userPermissionRepository;
    private final ResourceLoader resourceLoader;

    public List<GetUserRouteResponseDto> getMyRoutes(Long userId) {
        List<UserRoute> userRoutes = userRouteRepository.findAllByUserId(userId);
        List<Long> trrIds = userRoutes.stream().map(UserRoute::getTenantRoleRouteId).toList();
        if (trrIds.size() != 1) {
            throw new BizException(UserRouteErrorResponse.USR0002, trrIds);
        }
        Long trrId = trrIds.get(0);
        if (trrId == 0L) {
            // QUICK RETURN
            // ASSUME by-pass trrId. such as [referrer....]
            return userRoutes.stream()
                    .map(userRoute -> DtoWrapper.getGetUserRouteResponseDto(userRoute, null))
                    .toList();
        }
        Object tenantContext = RetrofitCallHandler.execute(tenantApiClient.getTenantContextByTrrId(trrId));
        return userRoutes.stream()
                .map(userRoute -> DtoWrapper.getGetUserRouteResponseDto(userRoute, tenantContext))
                .toList();
    }

    private GetUserResponseDto getMyMetadata(Long userId) {
        return uaaService.get(userId);
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