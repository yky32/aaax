package com.aaax.endpoint.api.mgt;

import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.response.Pagination;
import com.aaax.core.response.PaginationDto;
import com.aaax.core.response.Result;
import com.aaax.entity.dto.request.UpdatePasswordRequestDto;
import com.aaax.entity.dto.request.UpdateUserStatusRequestDto;
import com.aaax.usecase.RegisterUserUseCase;
import com.aaax.usecase.UserManagementUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MgtUserEndpointTest {

    @Mock private UserManagementUseCase userManagementUseCase;
    @Mock private RegisterUserUseCase registerUserUseCase;

    @InjectMocks
    private MgtUserEndpoint endpoint;

    @Test
    @DisplayName("updateCredentials should delegate")
    void updateCredentials_shouldDelegate() {
        UpdatePasswordRequestDto request = UpdatePasswordRequestDto.builder().build();
        when(userManagementUseCase.updateCredentials(request, "u@test.com"))
                .thenReturn(GetUserResponseDto.builder().id("1").build());
        assertEquals("1", endpoint.updateCredentials(request, "u@test.com").getData().getId());
    }

    @Test
    @DisplayName("updateStatuses should validate status and delegate")
    void updateStatuses_shouldDelegate() {
        UpdateUserStatusRequestDto request = UpdateUserStatusRequestDto.builder().status("ACTIVE").build();
        when(userManagementUseCase.updateStatuses(request, "u@test.com"))
                .thenReturn(GetUserResponseDto.builder().id("1").build());
        assertEquals("1", endpoint.updateStatuses(request, "u@test.com").getData().getId());
    }

    @Test
    @DisplayName("getAllUsers should return paginated list")
    void getAllUsers_shouldDelegate() {
        Pagination pagination = new Pagination();
        pagination.setTotal(1L);
        PaginationDto.PaginationDtoBuilder<List<GetUserResponseDto>> builder = PaginationDto.<List<GetUserResponseDto>>builder()
                .data(List.of(GetUserResponseDto.builder().id("1").build()))
                .pagination(pagination);
        when(userManagementUseCase.getAllUsers(any(), any(), any(), any(), any())).thenReturn(builder);

        Result<List<GetUserResponseDto>> result = endpoint.getAllUsers(
                PageRequest.of(0, 10), null, null, null, null);
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("deleteUserByUsername should soft-delete false by default")
    void deleteUserByUsername_shouldDelegate() {
        Result<String> result = endpoint.deleteUserByUsername("u@test.com", null);
        verify(userManagementUseCase).deleteByIdentifier("u@test.com", false);
        assertTrue(result.getData().contains("deleted"));
    }

    @Test
    @DisplayName("deleteUserByUserId should pass soft delete flag")
    void deleteUserByUserId_shouldDelegate() {
        Result<String> result = endpoint.deleteUserByUserId("1", true);
        verify(userManagementUseCase).deleteByUserId("1", true);
        assertTrue(result.getData().contains("isSoftDelete"));
    }

    @Test
    @DisplayName("deleteUserByInternalTestingUserId should report count")
    void deleteInternalTesting_shouldDelegate() {
        when(userManagementUseCase.testingDeleteAll()).thenReturn(3);
        assertTrue(endpoint.deleteUserByInternalTestingUserId().getData().contains("3"));
    }
}
