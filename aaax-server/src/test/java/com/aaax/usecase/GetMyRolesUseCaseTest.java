package com.aaax.usecase;

import com.aaax.core.utils.RedisUtil;
import com.aaax.entity.dto.response.GetMyRolesResponseDto;
import com.aaax.entity.po.user_management.UserPermission;
import com.aaax.repository.UserPermissionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetMyRolesUseCaseTest {

    @Mock
    private UserPermissionRepository userPermissionRepository;

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private GetMyRolesUseCase getMyRolesUseCase;

    @Test
    @DisplayName("should return cached roles when Redis GET hits")
    void shouldReturnCachedRoles() {
        GetMyRolesResponseDto cached = GetMyRolesResponseDto.builder()
                .userId("u_1").roles(List.of("normal")).updatedAt(Instant.now()).build();
        when(redisUtil.get(anyString())).thenReturn(cached);

        GetMyRolesResponseDto result = getMyRolesUseCase.execute("u_1");

        assertNotNull(result);
        assertEquals(1, result.getRoles().size());
        verify(userPermissionRepository, never()).findByUserId(anyLong());
        verify(redisUtil, never()).hasKey(anyString());
    }

    @Test
    @DisplayName("should fallback to DB when Redis miss or type mismatch")
    void shouldFallbackToDb() {
        when(redisUtil.get(anyString())).thenReturn(null);
        UserPermission permission = UserPermission.builder()
                .id(1L).userId(1L).apiVersion("1.0")
                .actualPermissions(Map.of("normal", Map.of("sample_feature_key", Map.of("effect", "ALLOW", "authorities", List.of("READ", "WRITE")))))
                .build();
        when(userPermissionRepository.findByUserId(anyLong())).thenReturn(Optional.of(permission));

        GetMyRolesResponseDto result = getMyRolesUseCase.execute("u_1");

        assertNotNull(result);
        assertTrue(result.getRoles().contains("normal"));
        verify(redisUtil).set(anyString(), any(), anyLong());
    }

    @Test
    @DisplayName("should return empty roles when no UserPermission exists")
    void shouldReturnEmptyRolesWhenMissing() {
        when(redisUtil.get(anyString())).thenReturn(null);
        when(userPermissionRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

        GetMyRolesResponseDto result = getMyRolesUseCase.execute("u_1");

        assertNotNull(result);
        assertTrue(result.getRoles().isEmpty());
    }
}
