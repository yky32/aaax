package com.aaax.usecase;

import com.aaax.core.exception.BizException;
import com.aaax.core.utils.IdSplitter;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.core.utils.RetrofitCallHandler;
import com.aaax.config.redis.RedisKey;
import com.aaax.entity.dto.request.CreateUserRouteMgtRequestDto;
import com.aaax.entity.dto.response.GetUserRouteResponseDto;
import com.aaax.entity.po.UserRoute;
import com.aaax.entity.po.user.User;
import com.aaax.exception.response.UaaErrorResponse;
import com.aaax.exception.response.UserRouteErrorResponse;
import com.aaax.ext.api.client.tenant.TenantApiClient;
import com.aaax.repository.UserRepository;
import com.aaax.repository.UserRouteRepository;
import com.aaax.service.DtoWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRouteUseCase {

    private final UserRepository userRepository;
    private final UserRouteRepository userRouteRepository;
    private final TenantApiClient tenantApiClient;
    private final RedisUtil redisUtil;

    @Transactional
    public GetUserRouteResponseDto createUserRoute(String userId, CreateUserRouteMgtRequestDto requestDto) {
        User user = userRepository.findById(Long.valueOf(IdSplitter.split(userId))).orElseThrow(() -> new BizException(UaaErrorResponse.UAA0001, Map.of("userId", userId)));
        if (StringUtils.isBlank(requestDto.getTenantRoleRouteId())) { // case for referrer. no [trr-id]
            String name = "REFERRER";
            List<Map> result = JSONUtil.convertFromObject(RetrofitCallHandler.execute(tenantApiClient.getAllRouteTemplates(name)), new TypeReference<>() {});
            if (result.isEmpty()) {
                throw new BizException(UserRouteErrorResponse.USR0001, "No Route Templates in Tenant =>".concat(name));
            }
            UserRoute userRoute = UserRoute.builder()
                    .userId(user.getId())
                    .tenantRoleRouteId(0L)
                    .actualRoutes(result.get(0).getOrDefault("routes", Map.of()))
                    .build();
            userRoute = userRouteRepository.save(userRoute);
            return DtoWrapper.getGetUserRouteResponseDto(userRoute, null);
        }

        Long tenantRoleRouteId = Long.valueOf(requestDto.getTenantRoleRouteId());
        Map targetTrr = JSONUtil.convertFromObject(RetrofitCallHandler.execute(tenantApiClient.getTenantRoleRoute(tenantRoleRouteId)), new TypeReference<>() {});
        Object actualRoutes;
        String name = targetTrr.getOrDefault("role", "").toString();
        List<Map> result = JSONUtil.convertFromObject(RetrofitCallHandler.execute(tenantApiClient.getAllRouteTemplates(name)), new TypeReference<>() {});
        if (!result.isEmpty()) {
            actualRoutes = result.get(0).getOrDefault("routes", Map.of());
        } else {
            String routeTemplateId = (String) targetTrr.getOrDefault("routeTemplateId", "999");
            Map routeTemplate = JSONUtil.convertFromObject(RetrofitCallHandler.execute(tenantApiClient.getOneRouteTemplates(Long.valueOf(routeTemplateId))), new TypeReference<>() {});
            actualRoutes = routeTemplate.getOrDefault("routes", Map.of());
        }

        // === Validation:
        UserRoute userRoute;
        // === Validation: check this user is more than one trr existed
        Map<String, LinkedHashMap<String, Map>> existedTenantsWithTrr = new HashMap<>();
        List<UserRoute> userRoutes = userRouteRepository.findAllByUserId(user.getId());
        userRoutes = userRoutes.stream().sorted(Comparator.comparing(UserRoute::getUpdateDt).reversed()).toList();
        if (!userRoutes.isEmpty()) {
            for (UserRoute route : userRoutes) {
                Map trr = JSONUtil.convertFromObject(RetrofitCallHandler.execute(tenantApiClient.getTenantRoleRoute(route.getTenantRoleRouteId())), new TypeReference<>() {});
                String tenantId = (String) trr.get("tenantId");
                existedTenantsWithTrr.computeIfAbsent(tenantId, k -> new LinkedHashMap<>()).put((String) trr.get("id"), trr);
            }
        }
        LinkedHashMap<String, Map> existedTrrWithThisTenant = existedTenantsWithTrr.getOrDefault(targetTrr.get("tenantId"), new LinkedHashMap<>());
        if (existedTenantsWithTrr.size() > 1) {
            throw new BizException(UserRouteErrorResponse.USR0002, Map.of(
                    "message", "// more than one MAIN_TENANT ... invalid case",
                    "detail", existedTenantsWithTrr
            ));
        }
        if (existedTrrWithThisTenant.size() > 1) {
            throw new BizException(UserRouteErrorResponse.USR0002, Map.of(
                    "message", "// more than one route within same tenant",
                    "detail", existedTrrWithThisTenant
            ));
        }
        if (!existedTrrWithThisTenant.isEmpty()) {
            // UPSERT
            Map.Entry<String, Map> first = existedTrrWithThisTenant.entrySet().iterator().next();
            Optional<UserRoute> isExistedUserRoute = userRouteRepository.findByUserIdAndTenantRoleRouteId(user.getId(), Long.valueOf((String) first.getValue().get("id")));
            if (isExistedUserRoute.isPresent()) {
                userRoute = isExistedUserRoute.get();
                userRoute.setTenantRoleRouteId(tenantRoleRouteId);
                userRoute = userRouteRepository.save(userRoute);
                this.cleanupUserRoutes(userId);
                return DtoWrapper.getGetUserRouteResponseDto(userRoute, null);
            }
        }



        Optional<UserRoute> isExistedUserRoute = userRouteRepository.findByUserIdAndTenantRoleRouteId(user.getId(), tenantRoleRouteId);
        if (isExistedUserRoute.isPresent()) {
            userRoute = isExistedUserRoute.get();
        } else {
            userRoute = UserRoute.builder()
                    .userId(user.getId())
                    .tenantRoleRouteId(tenantRoleRouteId)
                    .actualRoutes(actualRoutes)
                    .build();
            userRoute = userRouteRepository.save(userRoute);
        }
        this.cleanupUserRoutes(userId);
        return DtoWrapper.getGetUserRouteResponseDto(userRoute, null);
    }

    private void cleanupUserRoutes(String userId) {
        String redisKey = RedisKey.LOGIN_MY_ROUTES.getKey().concat(userId);
        redisUtil.delete(redisKey);
    }
}
