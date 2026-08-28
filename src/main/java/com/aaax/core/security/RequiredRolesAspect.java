package com.aaax.core.security;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;
import com.aaax.core.utils.JwtUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Enforces {@link RequiredRoles} using live roles from AAAX so operator access
 * reflects hot-reloaded permissions without relying on immutable JWT scopes.
 * <p>
 * Register as a Spring {@code @Bean} (typically from each service's {@code AopConfig}).
 */
@Aspect
public class RequiredRolesAspect {

    private static final Logger log = LoggerFactory.getLogger(RequiredRolesAspect.class);

    private final OperatorRoleResolver roleResolver;

    public RequiredRolesAspect(OperatorRoleResolver roleResolver) {
        this.roleResolver = roleResolver;
    }

    @Before("@within(com.aaax.core.security.RequiredRoles) || @annotation(com.aaax.core.security.RequiredRoles)")
    public void enforceRequiredRoles(JoinPoint joinPoint) {
        RequiredRoles annotation = resolveRequiredRoles(joinPoint);
        if (annotation == null) {
            return;
        }

        String[] required = annotation.value();
        if (required.length == 0) {
            required = new String[]{roleResolver.operatorRoleName()};
        }

        if (roleResolver.hasAnyRole(required)) {
            return;
        }

        log.warn(
                "Access denied: JWT subject={} lacks any live role among {}",
                JwtUtil.userId(),
                Arrays.toString(required));
        throw new BizException(
                SystemResponse.SAU0401,
                "Required role not granted: " + String.join(", ", required));
    }

    private static RequiredRoles resolveRequiredRoles(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequiredRoles onMethod = AnnotationUtils.findAnnotation(method, RequiredRoles.class);
        if (onMethod != null) {
            return onMethod;
        }
        return AnnotationUtils.findAnnotation(method.getDeclaringClass(), RequiredRoles.class);
    }
}
