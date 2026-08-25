package com.aaax.usecase;

import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.entity.po.UserRoute;
import com.aaax.entity.po.user.Authentication;
import com.aaax.entity.po.user.User;
import com.aaax.repository.AuthenticationRepository;
import com.aaax.repository.UserRepository;
import com.aaax.repository.UserRouteRepository;
import com.aaax.service.UaaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetUserUseCaseTest {

    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UaaService uaaService;
    @Mock private UserRepository userRepository;
    @Mock private AuthenticationRepository authenticationRepository;
    @Mock private UserRouteRepository userRouteRepository;

    @InjectMocks
    private GetUserUseCase getUserUseCase;

    @Test
    @DisplayName("execute(Long) should delegate to uaaService")
    void execute_byId_shouldDelegate() {
        when(uaaService.get(1L)).thenReturn(GetUserResponseDto.builder().id("1").build());
        assertEquals("1", getUserUseCase.execute(1L).getId());
    }

    @Test
    @DisplayName("execute(String) should lookup by username")
    void execute_byIdentifier_shouldDelegate() {
        Authentication auth = Authentication.builder().identifier("u@test.com").build();
        when(uaaService.getByUsername("u@test.com")).thenReturn(auth);
        assertEquals("u@test.com", getUserUseCase.execute("u@test.com").getIdentifier());
    }

    @Test
    @DisplayName("searchByTrrIds should map users from routes")
    void searchByTrrIds_shouldMapUsers() {
        when(userRouteRepository.findAllByTenantRoleRouteIdIn(List.of(9L)))
                .thenReturn(List.of(UserRoute.builder().userId(5L).tenantRoleRouteId(9L).build()));
        User user = User.builder().id(5L).username("u@test.com").authentications(List.of()).build();
        when(userRepository.findAllById(List.of(5L))).thenReturn(List.of(user));

        List<GetUserResponseDto> result = getUserUseCase.searchByTrrIds(List.of("9"));
        assertEquals(1, result.size());
        assertEquals("u_5", result.get(0).getId());
    }

    @Test
    @DisplayName("getTrrIds should return tenant role route ids")
    void getTrrIds_shouldReturnIds() {
        when(userRouteRepository.findAllByUserId(5L))
                .thenReturn(List.of(UserRoute.builder().tenantRoleRouteId(9L).build()));
        assertEquals(List.of(9L), getUserUseCase.getTrrIds("5"));
    }
}
