package com.aaax.core.security;

import com.aaax.core.api.AaaxApiClient;
import com.aaax.core.entity.dto.aaax.response.GetMyRolesResponseDto;
import com.aaax.core.utils.RetrofitCallHandler;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Resolves live roles from AAAX ({@code GET /users/my-roles}) so operator access
 * reflects hot-reloaded permissions without relying on immutable JWT scopes.
 * <p>
 * Services should register this as a {@code @Bean}, injecting the user-token
 * {@link AaaxApiClient} (typically {@code @Qualifier("aaaxApiClient")}).
 */
public class OperatorRoleResolver {

    private final AaaxApiClient aaaxApiClient;
    private final String operatorRole;

    public OperatorRoleResolver(AaaxApiClient aaaxApiClient, String operatorRole) {
        this.aaaxApiClient = aaaxApiClient;
        this.operatorRole = operatorRole;
    }

    public OperatorRoleResolver(AaaxApiClient aaaxApiClient) {
        this(aaaxApiClient, OperatorRoles.ADMIN);
    }

    public List<String> currentUserRoles() {
        GetMyRolesResponseDto response = RetrofitCallHandler.execute(aaaxApiClient.getMyRoles());
        if (response == null || response.getRoles() == null) {
            return Collections.emptyList();
        }
        return response.getRoles();
    }

    public boolean hasOperatorRole() {
        return hasAnyRole(operatorRoleName());
    }

    /** True when the caller holds at least one of the given live AAAX roles. */
    public boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return true;
        }
        List<String> live = currentUserRoles();
        return Stream.of(roles).anyMatch(live::contains);
    }

    public String operatorRoleName() {
        return operatorRole == null || operatorRole.isBlank()
                ? OperatorRoles.ADMIN
                : operatorRole;
    }
}
