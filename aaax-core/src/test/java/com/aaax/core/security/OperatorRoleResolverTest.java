package com.aaax.core.security;

import com.aaax.core.api.UaaApiClient;
import com.aaax.core.entity.dto.uaa.response.GetMyRolesResponseDto;
import com.aaax.core.utils.RetrofitCallHandler;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class OperatorRoleResolverTest {

    @Test
    void hasAnyRole_trueWhenOneRoleMatches() {
        UaaApiClient uaaApiClient = mock(UaaApiClient.class);
        when(uaaApiClient.getMyRoles()).thenReturn(null);
        OperatorRoleResolver resolver = new OperatorRoleResolver(uaaApiClient);
        GetMyRolesResponseDto dto = GetMyRolesResponseDto.builder()
                .roles(List.of("normal", OperatorRoles.ADMIN))
                .build();

        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler.execute(any())).thenReturn(dto);
            assertTrue(resolver.hasAnyRole("other", OperatorRoles.ADMIN));
        }
    }

    @Test
    void hasOperatorRole_trueWhenRolePresent() {
        UaaApiClient uaaApiClient = mock(UaaApiClient.class);
        when(uaaApiClient.getMyRoles()).thenReturn(null);
        OperatorRoleResolver resolver = new OperatorRoleResolver(uaaApiClient);
        GetMyRolesResponseDto dto = GetMyRolesResponseDto.builder()
                .roles(List.of("normal", OperatorRoles.ADMIN))
                .build();

        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler.execute(any())).thenReturn(dto);
            assertTrue(resolver.hasOperatorRole());
        }
    }

    @Test
    void hasOperatorRole_falseWhenRoleMissing() {
        UaaApiClient uaaApiClient = mock(UaaApiClient.class);
        when(uaaApiClient.getMyRoles()).thenReturn(null);
        OperatorRoleResolver resolver = new OperatorRoleResolver(uaaApiClient);
        GetMyRolesResponseDto dto = GetMyRolesResponseDto.builder()
                .roles(List.of("normal"))
                .build();

        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler.execute(any())).thenReturn(dto);
            assertFalse(resolver.hasOperatorRole());
        }
    }

    @Test
    void operatorRoleName_usesConfiguredValue() {
        OperatorRoleResolver resolver = new OperatorRoleResolver(mock(UaaApiClient.class), "custom_operator");
        assertEquals("custom_operator", resolver.operatorRoleName());
    }
}
