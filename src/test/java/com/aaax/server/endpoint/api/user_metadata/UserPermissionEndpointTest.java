package com.aaax.server.endpoint.api.user_metadata;

import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.event.UserPermissionMutatedEvent;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.server.entity.dto.request.AssignUserRoleRequestDto;
import com.aaax.server.entity.dto.response.GetUserPermissionResponseDto;
import com.aaax.server.usecase.AccessControlUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPermissionEndpointTest {

    @Mock private AccessControlUseCase accessControlUseCase;
    @Mock private KafkaUtil kafkaUtil;

    @InjectMocks
    private UserPermissionEndpoint endpoint;

    @Test
    @DisplayName("assignUserPermissions should delegate")
    void assignUserPermissions_shouldDelegate() {
        UserPermissionMutatedEvent event = UserPermissionMutatedEvent.builder().userId("1").build();
        when(accessControlUseCase.assignPermissionToUser(event))
                .thenReturn(GetUserPermissionResponseDto.builder().id("p1").build());
        assertEquals("p1", endpoint.assignUserPermissions(event).getData().getId());
    }

    @Test
    @DisplayName("assignUserRoles should reject unknown role")
    void assignUserRoles_shouldRejectUnknownRole() {
        AssignUserRoleRequestDto dto = AssignUserRoleRequestDto.builder().roles(List.of("superuser")).build();
        assertThrows(BizException.class, () -> endpoint.assignUserRoles("1", dto));
        verify(accessControlUseCase, never()).assignRoleToUser(any(), any());
    }

    @Test
    @DisplayName("assignUserRoles should accept predefined roles")
    void assignUserRoles_shouldAcceptPredefined() {
        AssignUserRoleRequestDto dto = AssignUserRoleRequestDto.builder().roles(List.of("admin")).build();
        when(accessControlUseCase.assignRoleToUser(eq("1"), any()))
                .thenReturn(GetUserPermissionResponseDto.builder().id("p2").build());
        assertEquals("p2", endpoint.assignUserRoles("1", dto).getData().getId());
    }
}
