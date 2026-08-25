package com.aaax.core.security;

import com.aaax.core.api.UaaApiClient;
import com.aaax.core.entity.dto.uaa.response.GetMyRolesResponseDto;
import com.aaax.core.utils.RetrofitCallHandler;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Resolves live roles from UAA ({@code GET /users/my-roles}) so operator access
 * reflects hot-reloaded permissions without relying on immutable JWT scopes.
 * <p>
 * Services should register this as a {@code @Bean}, injecting the user-token
 * {@link UaaApiClient} (typically {@code @Qualifier("uaaApiClient")}).
 */
public class OperatorRoleResolver {

    private final UaaApiClient uaaApiClient;
    private final String operatorRole;

    public OperatorRoleResolver(UaaApiClient uaaApiClient, String operatorRole) {
        this.uaaApiClient = uaaApiClient;
        this.operatorRole = operatorRole;
    }

    public OperatorRoleResolver(UaaApiClient uaaApiClient) {
        this(uaaApiClient, OperatorRoles.ADMIN);
    }

    public List<String> currentUserRoles() {
        GetMyRolesResponseDto response = RetrofitCallHandler.execute(uaaApiClient.getMyRoles());
        if (response == null || response.getRoles() == null) {
            return Collections.emptyList();
        }
        return response.getRoles();
    }

    public boolean hasOperatorRole() {
        return hasAnyRole(operatorRoleName());
    }

    /** True when the caller holds at least one of the given live UAA roles. */
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
