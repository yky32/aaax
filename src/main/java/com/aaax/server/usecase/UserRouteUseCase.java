package com.aaax.server.usecase;

import java.util.Map;
import java.util.Optional;

import com.aaax.core.exception.BizException;
import com.aaax.core.utils.IdSplitter;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.config.redis.RedisKey;
import com.aaax.server.entity.dto.request.CreateUserRouteMgtRequestDto;
import com.aaax.server.entity.dto.response.GetUserRouteResponseDto;
import com.aaax.server.entity.po.UserRoute;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.exception.response.AaaxErrorResponse;
import com.aaax.server.repository.UserRepository;
import com.aaax.server.repository.UserRouteRepository;
import com.aaax.server.service.DtoWrapper;

import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Local user-route storage only. Tenant-service template fetch removed for OSS.
 * Clients may pass {@code tenantRoleRouteId} as an opaque external id + optional routes payload later.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserRouteUseCase {

    private final UserRepository userRepository;
    private final UserRouteRepository userRouteRepository;
    private final RedisUtil redisUtil;

    @Transactional
    public GetUserRouteResponseDto createUserRoute(String userId, CreateUserRouteMgtRequestDto requestDto) {
        User user = userRepository
                .findById(Long.valueOf(IdSplitter.split(userId)))
                .orElseThrow(() -> new BizException(AaaxErrorResponse.AAAX0001, Map.of("userId", userId)));

        long trrId = 0L;
        if (StringUtils.isNotBlank(requestDto.getTenantRoleRouteId())) {
            trrId = Long.parseLong(requestDto.getTenantRoleRouteId());
        }

        Optional<UserRoute> existing = userRouteRepository.findByUserIdAndTenantRoleRouteId(user.getId(), trrId);
        UserRoute userRoute;
        if (existing.isPresent()) {
            userRoute = existing.get();
        } else {
            userRoute = UserRoute.builder()
                    .userId(user.getId())
                    .tenantRoleRouteId(trrId)
                    .actualRoutes(Map.of())
                    .build();
            userRoute = userRouteRepository.save(userRoute);
        }
        cleanupUserRoutes(userId);
        return DtoWrapper.getGetUserRouteResponseDto(userRoute, null);
    }

    private void cleanupUserRoutes(String userId) {
        String redisKey = RedisKey.LOGIN_MY_ROUTES.getKey().concat(userId);
        redisUtil.delete(redisKey);
    }
}
