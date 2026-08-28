package com.aaax.core.security;

import com.aaax.core.api.AaaxApiClient;
import com.aaax.core.entity.dto.aaax.response.GetMyRolesResponseDto;
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
        AaaxApiClient aaaxApiClient = mock(AaaxApiClient.class);
        when(aaaxApiClient.getMyRoles()).thenReturn(null);
        OperatorRoleResolver resolver = new OperatorRoleResolver(aaaxApiClient);
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
        AaaxApiClient aaaxApiClient = mock(AaaxApiClient.class);
        when(aaaxApiClient.getMyRoles()).thenReturn(null);
        OperatorRoleResolver resolver = new OperatorRoleResolver(aaaxApiClient);
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
        AaaxApiClient aaaxApiClient = mock(AaaxApiClient.class);
        when(aaaxApiClient.getMyRoles()).thenReturn(null);
        OperatorRoleResolver resolver = new OperatorRoleResolver(aaaxApiClient);
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
        OperatorRoleResolver resolver = new OperatorRoleResolver(mock(AaaxApiClient.class), "custom_operator");
        assertEquals("custom_operator", resolver.operatorRoleName());
    }
}
