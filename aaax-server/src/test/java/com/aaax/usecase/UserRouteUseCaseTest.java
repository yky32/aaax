package com.aaax.usecase;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.Result;
import com.aaax.core.utils.RedisUtil;
import com.aaax.core.utils.RetrofitCallHandler;
import com.aaax.entity.dto.request.CreateUserRouteMgtRequestDto;
import com.aaax.entity.dto.response.GetUserRouteResponseDto;
import com.aaax.entity.po.UserRoute;
import com.aaax.entity.po.user.User;
import com.aaax.ext.api.client.tenant.TenantApiClient;
import com.aaax.repository.UserRepository;
import com.aaax.repository.UserRouteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Call;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRouteUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private UserRouteRepository userRouteRepository;
    @Mock private TenantApiClient tenantApiClient;
    @Mock private RedisUtil redisUtil;
    @Mock private Call<Result<Object>> call;

    @InjectMocks
    private UserRouteUseCase userRouteUseCase;

    @Test
    @DisplayName("createUserRoute should throw when user missing")
    void createUserRoute_shouldThrowWhenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () ->
                userRouteUseCase.createUserRoute("99", CreateUserRouteMgtRequestDto.builder().build()));
    }

    @Test
    @DisplayName("createUserRoute referrer path should save route from template")
    void createUserRoute_referrerPath_shouldSave() {
        User user = User.builder().id(5L).username("u@test.com").build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(tenantApiClient.getAllRouteTemplates("REFERRER")).thenReturn(call);
        when(userRouteRepository.save(any())).thenAnswer(inv -> {
            UserRoute route = inv.getArgument(0);
            route.setId(1L);
            route.setCreateDt(Instant.now());
            route.setUpdateDt(Instant.now());
            return route;
        });

        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler.execute(any()))
                    .thenReturn(List.of(Map.of("routes", Map.of("home", "/"))));

            GetUserRouteResponseDto dto = userRouteUseCase.createUserRoute(
                    "5", CreateUserRouteMgtRequestDto.builder().tenantRoleRouteId("").build());

            assertNotNull(dto);
            verify(userRouteRepository).save(argThat(r -> r.getTenantRoleRouteId() == 0L));
        }
    }

    @Test
    @DisplayName("createUserRoute referrer path should fail when templates empty")
    void createUserRoute_referrerPath_shouldFailWhenEmpty() {
        User user = User.builder().id(5L).username("u@test.com").build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(tenantApiClient.getAllRouteTemplates("REFERRER")).thenReturn(call);

        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler.execute(any())).thenReturn(List.of());

            assertThrows(BizException.class, () ->
                    userRouteUseCase.createUserRoute("5",
                            CreateUserRouteMgtRequestDto.builder().tenantRoleRouteId(null).build()));
        }
    }

    @Test
    @DisplayName("createUserRoute should create new route from tenant role route")
    void createUserRoute_trrPath_shouldCreate() {
        User user = User.builder().id(5L).username("u@test.com").build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(tenantApiClient.getTenantRoleRoute(9L)).thenReturn(call);
        when(tenantApiClient.getAllRouteTemplates("ADMIN")).thenReturn(call);
        when(userRouteRepository.findAllByUserId(5L)).thenReturn(List.of());
        when(userRouteRepository.findByUserIdAndTenantRoleRouteId(5L, 9L)).thenReturn(Optional.empty());
        when(userRouteRepository.save(any())).thenAnswer(inv -> {
            UserRoute route = inv.getArgument(0);
            route.setId(3L);
            route.setCreateDt(Instant.now());
            route.setUpdateDt(Instant.now());
            return route;
        });

        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler.execute(any()))
                    .thenReturn(Map.of("id", "9", "tenantId", "t1", "role", "ADMIN", "routeTemplateId", "1"))
                    .thenReturn(List.of(Map.of("routes", Map.of("home", "/"))));

            GetUserRouteResponseDto dto = userRouteUseCase.createUserRoute(
                    "5", CreateUserRouteMgtRequestDto.builder().tenantRoleRouteId("9").build());

            assertNotNull(dto);
            verify(redisUtil).delete(contains("5"));
        }
    }
}
