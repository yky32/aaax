package com.aaax.core.security;

import com.aaax.core.api.AaaxApiClient;
import com.aaax.core.exception.BizException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequiredRolesAspectTest {

    @Test
    void allowsWhenLiveRolePresent() {
        RequiredRolesAspect aspect = new RequiredRolesAspect(stubResolver(true, OperatorRoles.ADMIN));
        assertDoesNotThrow(() -> aspect.enforceRequiredRoles(joinPointFor(StubEndpoint.class)));
    }

    @Test
    void deniesWhenLiveRoleMissing() {
        RequiredRolesAspect aspect = new RequiredRolesAspect(stubResolver(false, OperatorRoles.ADMIN));
        assertThrows(BizException.class, () -> aspect.enforceRequiredRoles(joinPointFor(StubEndpoint.class)));
    }

    @Test
    void usesConfiguredOperatorRoleWhenAnnotationValueEmpty() {
        RequiredRolesAspect aspect = new RequiredRolesAspect(stubResolver(true, "custom_operator"));
        assertDoesNotThrow(() -> aspect.enforceRequiredRoles(joinPointFor(StubDefaultRoleEndpoint.class)));
    }

    @RequiredRoles(OperatorRoles.ADMIN)
    private static class StubEndpoint {
        void handle() {}
    }

    @RequiredRoles
    private static class StubDefaultRoleEndpoint {
        void handle() {}
    }

    private static OperatorRoleResolver stubResolver(boolean allow, String operatorRole) {
        return new OperatorRoleResolver(mock(AaaxApiClient.class), operatorRole) {
            @Override
            public boolean hasAnyRole(String... roles) {
                if (!allow) {
                    return false;
                }
                Set<String> required = new HashSet<>(Arrays.asList(roles));
                return required.contains(operatorRole) || required.isEmpty();
            }

            @Override
            public String operatorRoleName() {
                return operatorRole;
            }
        };
    }

    private static JoinPoint joinPointFor(Class<?> type) {
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        try {
            when(signature.getMethod()).thenReturn(type.getDeclaredMethod("handle"));
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
        return joinPoint;
    }
}
