package com.aaax.usecase;

import com.aaax.core.common.jsonfield.UserMetadata;
import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.utils.RedisUtil;
import com.aaax.entity.dto.request.UpdateAccessMetadataRequestDto;
import com.aaax.entity.dto.request.UpdateExtReferenceRequestDto;
import com.aaax.entity.po.user.User;
import com.aaax.repository.UserRepository;
import com.aaax.service.UaaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateUserMetadataUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private UaaService uaaService;
    @Mock private RedisUtil redisUtil;

    @InjectMocks
    private UpdateUserMetadataUseCase updateUserMetadataUseCase;

    @Test
    @DisplayName("updateExtReference should persist metadata map")
    void updateExtReference_shouldPersist() {
        UserMetadata metadata = new UserMetadata();
        User user = User.builder().id(1L).username("u").status(UserStatus.ACTIVE).metadata(metadata).build();
        user.setAuthentications(List.of());
        when(uaaService.getById(1L)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        Map<String, Object> ext = Map.of("ext", "ref-1");
        GetUserResponseDto result = updateUserMetadataUseCase.updateExtReference(
                UpdateExtReferenceRequestDto.builder().extReferenceMap(ext).build(), 1L);

        assertEquals(ext, user.getMetadata().getExtReferenceMap());
        assertEquals("u_1", result.getId());
    }

    @Test
    @DisplayName("updateAccess should clear redis tenant caches")
    void updateAccess_shouldClearCaches() {
        User user = User.builder().id(2L).username("u2").status(UserStatus.ACTIVE).build();
        user.setAuthentications(List.of());
        when(uaaService.getById("u_2")).thenReturn(user);
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(redisUtil.getListByWildCard(anyString())).thenReturn(Set.of("user:login-my-tenants:1", "user:login-my-tenants:2"));

        GetUserResponseDto result = updateUserMetadataUseCase.updateAccess(
                UpdateAccessMetadataRequestDto.builder().access(new HashMap<>()).build(), "u_2");

        assertEquals("u_2", result.getId());
        verify(redisUtil, times(2)).delete(anyString());
    }

    @Test
    @DisplayName("updateAccess should reject null access")
    void updateAccess_shouldRejectNullAccess() {
        assertThrows(NullPointerException.class, () -> updateUserMetadataUseCase.updateAccess(
                UpdateAccessMetadataRequestDto.builder().access(null).build(), "u_1"));
    }
}
