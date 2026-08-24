package com.aaax.endpoint.api.my_login_profile;

import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.utils.IdSplitter;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.JwtUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.config.redis.RedisKey;
import com.aaax.entity.dto.response.GetUserPermissionResponseDto;
import com.aaax.entity.dto.response.GetUserRouteResponseDto;
import com.aaax.usecase.GetMyLoginProfileUseCase;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class MyLoginProfileEndpoint {

    private final GetMyLoginProfileUseCase getMyLoginProfileUseCase;
    private final RedisUtil redisUtil;

    // ____ user-routes :  userId--> jwt
    @GetMapping("/users/my-routes")
    public Result<List<GetUserRouteResponseDto>> getMyRoutes() {
        String userId = JwtUtil.userId();
        String redisKey = RedisKey.LOGIN_MY_ROUTES.getKey().concat(userId);
        List<GetUserRouteResponseDto> myRoutes;
        try {
            // P0: single GET
            Object _myRoutes = redisUtil.get(redisKey);
            if (_myRoutes != null) {
                myRoutes = JSONUtil.convertFromObject(_myRoutes, new TypeReference<>() {});
                return R.success(myRoutes);
            }
        } catch (Exception exception) {
            log.info("--- ==== /users/my-routes === Error in Redis... => {}", exception.getMessage());
            return this._getMyRoutes(userId, redisKey); // in-case plan b
        }
        return this._getMyRoutes(userId, redisKey);
    }

    @GetMapping("/mgt/users/{userId}/routes")
    public Result<List<GetUserRouteResponseDto>> getUserRouteOfThisUser(
            @PathVariable String userId
    ) {
        String redisKey = RedisKey.LOGIN_MY_ROUTES.getKey().concat(userId);
        List<GetUserRouteResponseDto> myRoutes;
        try {
            // P0: single GET
            Object _myRoutes = redisUtil.get(redisKey);
            if (_myRoutes != null) {
                myRoutes = JSONUtil.convertFromObject(_myRoutes, new TypeReference<>() {});
                return R.success(myRoutes);
            }
        } catch (Exception exception) {
            log.info("--- ==== /mgt/users/{}/routes === Error in Redis... => {}", userId, exception.getMessage());
            return this._getMyRoutes(userId, redisKey); // in-case plan b
        }
        return this._getMyRoutes(userId, redisKey);
    }

    private @NotNull Result<List<GetUserRouteResponseDto>> _getMyRoutes(String userId, String redisKey) {
        List<GetUserRouteResponseDto> myRoutes = getMyLoginProfileUseCase.getMyRoutes(Long.valueOf(IdSplitter.split(userId)));
        redisUtil.set(redisKey, myRoutes, 30);
        return R.success(myRoutes);
    }


    @GetMapping("/users/permissions/my-permissions")
    public Result<GetUserPermissionResponseDto> getMyPermissions() {
        String userId = JwtUtil.userId();
        String redisKey = RedisKey.LOGIN_MY_PERMISSIONS.getKey().concat(userId);
        GetUserPermissionResponseDto myPermissions;
        try {
            // P0: single GET
            Object _myPermissions = redisUtil.get(redisKey);
            if (_myPermissions != null) {
                myPermissions = JSONUtil.convertFromObject(_myPermissions, new TypeReference<>() {});
                return R.success(myPermissions);
            }
        } catch (Exception exception) {
            log.info("--- ==== /users/my-permissions === Error in Redis... => {}", exception.getMessage());
            return this._getMyPermissions(userId, redisKey); // in-case plan b
        }
        return this._getMyPermissions(userId, redisKey);
    }

    @GetMapping("/mgt/users/{userId}/permissions")
    public Result<GetUserPermissionResponseDto> getUserPermissionsOfThisUser(
            @PathVariable String userId
    ) {
        String redisKey = RedisKey.LOGIN_MY_PERMISSIONS.getKey().concat(userId);
        GetUserPermissionResponseDto myPermissions;
        try {
            // P0: single GET
            Object _myPermissions = redisUtil.get(redisKey);
            if (_myPermissions != null) {
                myPermissions = JSONUtil.convertFromObject(_myPermissions, new TypeReference<>() {});
                return R.success(myPermissions);
            }
        } catch (Exception exception) {
            log.info("--- ==== /mgt/users/{}/permissions === Error in Redis... => {}", userId, exception.getMessage());
            return this._getMyPermissions(userId, redisKey); // in-case plan b
        }
        return this._getMyPermissions(userId, redisKey);
    }

    private Result<GetUserPermissionResponseDto> _getMyPermissions(String userId, String redisKey) {
        GetUserPermissionResponseDto myPermissions = getMyLoginProfileUseCase.getMyPermissions(
                Long.valueOf(IdSplitter.split(userId)));
        myPermissions.setLastModifiedDt(Instant.now()); // LET UI know whether its lastModified.
        redisUtil.set(redisKey, myPermissions, 30);
        return R.success(myPermissions);
    }
}
