package com.aaax.server.usecase;

import com.aaax.core.exception.BizException;
import com.aaax.core.utils.IdSplitter;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.config.redis.RedisKey;
import com.aaax.server.entity.dto.response.GetMyRolesResponseDto;
import com.aaax.server.entity.po.user_management.UserPermission;
import com.aaax.server.repository.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetMyRolesUseCase {

    private final UserPermissionRepository userPermissionRepository;
    private final RedisUtil redisUtil;

    public GetMyRolesResponseDto execute(String userId) {
        String redisKey = RedisKey.LOGIN_MY_ROLES.getKey().concat(userId);
        try {
            // P0: single GET (avoid hasKey + get = 2 commands)
            Object cached = redisUtil.get(redisKey);
            if (cached instanceof GetMyRolesResponseDto dto) {
                return dto;
            }
        } catch (Exception e) {
            log.info("--- ==== getMyRoles === Error in Redis... => {}", e.getMessage());
        }
        return this.buildFromDb(userId, redisKey);
    }

    private GetMyRolesResponseDto buildFromDb(String userId, String redisKey) {
        Optional<UserPermission> optional = userPermissionRepository.findByUserId(Long.valueOf(IdSplitter.split(userId)));
        UserPermission permission = optional.orElseGet(() ->
                UserPermission.builder()
                        .apiVersion("1.0")
                        .userId(Long.valueOf(IdSplitter.split(userId)))
                        .actualPermissions(Map.of())
                        .build()
        );
        Map<String, Object> actualPermissions = permission.getActualPermissions();
        List<String> roles = actualPermissions.keySet().stream().toList();
        Instant updatedAt = Optional.ofNullable(permission.getUpdateDt()).orElse(permission.getCreateDt());
        GetMyRolesResponseDto dto = GetMyRolesResponseDto.builder()
                .userId("u_" + permission.getUserId())
                .roles(roles)
                .updatedAt(updatedAt)
                .build();
        try {
            redisUtil.set(redisKey, dto, 30);
        } catch (Exception e) {
            log.info("--- ==== getMyRoles === set Redis failed, continue... => {}", e.getMessage());
        }
        return dto;
    }
}
