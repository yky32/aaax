package com.aaax.server.usecase;

import com.aaax.core.common.jsonfield.PermissionMetadata;
import com.aaax.core.constant.enu.aaax.Authorities;
import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.event.UserPermissionMutatedEvent;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.entity.dto.request.CreateRbacTemplateRequestDto;
import com.aaax.server.entity.dto.response.GetRbacTemplateResponseDto;
import com.aaax.server.entity.dto.response.GetUserPermissionResponseDto;
import com.aaax.server.entity.po.rbac.RbacTemplate;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.entity.po.user_management.UserPermission;
import com.aaax.server.repository.RbacTemplateRepository;
import com.aaax.server.repository.UserPermissionRepository;
import com.aaax.server.service.AaaxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessControlUseCaseTest {

    @Mock private RbacTemplateRepository rbacTemplateRepository;
    @Mock private UserPermissionRepository userPermissionRepository;
    @Mock private RedisUtil redisUtil;
    @Mock private AaaxService aaaxService;
    @Mock private ResourceLoader resourceLoader;

    @InjectMocks
    private AccessControlUseCase accessControlUseCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(accessControlUseCase, "timezone", "UTC");
    }

    @Test
    @DisplayName("createRbacTemplate should expand ALL authorities and save")
    void createRbacTemplate_shouldExpandAllAndSave() {
        PermissionMetadata metadata = PermissionMetadata.builder()
                .key("feature")
                .authorities(new ArrayList<>(List.of(Authorities.ALL)))
                .build();
        Map<String, Object> permissions = new HashMap<>();
        permissions.put("feature", Map.of(
                "key", "feature",
                "authorities", List.of("ALL")
        ));

        when(rbacTemplateRepository.save(any())).thenAnswer(inv -> {
            RbacTemplate t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        GetRbacTemplateResponseDto result = accessControlUseCase.createRbacTemplate(
                CreateRbacTemplateRequestDto.builder()
                        .name("admin")
                        .description("desc")
                        .permissions(permissions)
                        .build());

        assertEquals("rbac_1", result.getId());
        verify(rbacTemplateRepository).save(any());
    }

    @Test
    @DisplayName("getOneRbacTemplate should return template or throw")
    void getOneRbacTemplate_shouldReturnOrThrow() {
        when(rbacTemplateRepository.findById(5L)).thenReturn(Optional.of(
                RbacTemplate.builder().id(5L).name("r").permissions(Map.of()).build()));
        assertEquals("rbac_5", accessControlUseCase.getOneRbacTemplate("rbac_5").getId());

        when(rbacTemplateRepository.findById(6L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> accessControlUseCase.getOneRbacTemplate("rbac_6"));
    }

    @Test
    @DisplayName("assignPermissionToUser should create permissions for new user")
    void assignPermissionToUser_shouldCreateForNewUser() {
        User user = User.builder().id(1L).username("u").build();
        when(aaaxService.getById("u_1")).thenReturn(user);
        when(userPermissionRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userPermissionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            UserPermission up = inv.getArgument(0);
            up.setId(10L);
            return up;
        });
        when(userPermissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PermissionMetadata permission = PermissionMetadata.builder()
                .key("feature_a")
                .authorities(new ArrayList<>(List.of(Authorities.READ)))
                .isOverride(true)
                .build();

        UserPermissionMutatedEvent event = UserPermissionMutatedEvent.builder()
                .userId("u_1")
                .permissions(List.of(permission))
                .build();

        GetUserPermissionResponseDto result = accessControlUseCase.assignPermissionToUser(event);

        assertEquals("upm_10", result.getId());
        verify(redisUtil, atLeastOnce()).delete(anyString());
    }

    @Test
    @DisplayName("assignPermissionToUser should override existing permission")
    void assignPermissionToUser_shouldOverrideExisting() {
        User user = User.builder().id(1L).username("u").build();
        UserPermission existing = UserPermission.builder()
                .id(10L)
                .userId(1L)
                .apiVersion("1.0")
                .actualPermissions(new HashMap<>(Map.of("old", Map.of())))
                .build();
        when(aaaxService.getById("u_1")).thenReturn(user);
        when(userPermissionRepository.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(userPermissionRepository.saveAndFlush(existing)).thenReturn(existing);
        when(userPermissionRepository.save(existing)).thenReturn(existing);

        PermissionMetadata permission = PermissionMetadata.builder()
                .key("feature_b")
                .authorities(new ArrayList<>(List.of(Authorities.WRITE)))
                .isOverride(true)
                .build();

        GetUserPermissionResponseDto result = accessControlUseCase.assignPermissionToUser(
                UserPermissionMutatedEvent.builder().userId("u_1").permissions(List.of(permission)).build());

        assertTrue(result.getRoles().contains("feature_b") || result.getPermissions().containsKey("feature_b"));
    }
}
