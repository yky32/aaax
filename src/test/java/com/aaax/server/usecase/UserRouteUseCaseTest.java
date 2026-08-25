package com.aaax.server.usecase;

import java.time.Instant;
import java.util.Optional;

import com.aaax.core.exception.BizException;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.entity.dto.request.CreateUserRouteMgtRequestDto;
import com.aaax.server.entity.dto.response.GetUserRouteResponseDto;
import com.aaax.server.entity.po.UserRoute;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.repository.UserRepository;
import com.aaax.server.repository.UserRouteRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRouteUseCaseTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRouteRepository userRouteRepository;
    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private UserRouteUseCase userRouteUseCase;

    @Test
    @DisplayName("createUserRoute should throw when user missing")
    void createUserRoute_shouldThrowWhenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(
                BizException.class,
                () -> userRouteUseCase.createUserRoute("99", CreateUserRouteMgtRequestDto.builder().build()));
    }

    @Test
    @DisplayName("createUserRoute local path should save without tenant mesh")
    void createUserRoute_local_shouldSave() {
        User user = User.builder().id(5L).username("u@test.com").build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRouteRepository.findByUserIdAndTenantRoleRouteId(5L, 0L)).thenReturn(Optional.empty());
        when(userRouteRepository.save(any())).thenAnswer(inv -> {
            UserRoute route = inv.getArgument(0);
            route.setId(1L);
            route.setCreateDt(Instant.now());
            route.setUpdateDt(Instant.now());
            return route;
        });

        GetUserRouteResponseDto dto = userRouteUseCase.createUserRoute(
                "5", CreateUserRouteMgtRequestDto.builder().tenantRoleRouteId("").build());

        assertNotNull(dto);
        verify(userRouteRepository).save(argThat(r -> r.getTenantRoleRouteId() == 0L));
        verify(redisUtil).delete(contains("5"));
    }

    @Test
    @DisplayName("createUserRoute with opaque trr id should save locally")
    void createUserRoute_withTrrId_shouldSave() {
        User user = User.builder().id(5L).username("u@test.com").build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRouteRepository.findByUserIdAndTenantRoleRouteId(5L, 9L)).thenReturn(Optional.empty());
        when(userRouteRepository.save(any())).thenAnswer(inv -> {
            UserRoute route = inv.getArgument(0);
            route.setId(3L);
            route.setCreateDt(Instant.now());
            route.setUpdateDt(Instant.now());
            return route;
        });

        GetUserRouteResponseDto dto = userRouteUseCase.createUserRoute(
                "5", CreateUserRouteMgtRequestDto.builder().tenantRoleRouteId("9").build());

        assertNotNull(dto);
        verify(userRouteRepository).save(argThat(r -> r.getTenantRoleRouteId() == 9L));
    }
}
