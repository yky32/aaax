package com.aaax.core.aop.authorization;

import com.aaax.core.aop.ThrowableBaseAspect;
import com.aaax.core.common.AppContext;
import com.aaax.core.common.AppContextHolder;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;
import com.aaax.core.utils.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.stream.Stream;


/**
 * Core-logic for produce the log.
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class RequiredPermissionAspect extends ThrowableBaseAspect {

    @Value("${spring.application.name}")
    private String serviceName;
    private final HttpServletRequest request;
    private final KafkaUtil kafkaUtil;


    @Before("@annotation(RequiredPermission)")
    public void requiredPermission_actionForBefore(JoinPoint joinPoint) {
        this.actionForBefore(RequiredPermission.class.getName(), joinPoint);
    }

    @Override
    public void executeForBefore(JoinPoint joinPoint) {
        RequiredPermission requiredPermission = AnnotationUtils.getAnnotation(ReflectionUtil.getMethod(joinPoint), RequiredPermission.class);
        AppContext appContext = AppContextHolder.CONTEXT.get();
        setUp(appContext, joinPoint, requiredPermission);

        // Check Over Permission
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        final Authentication authentication = securityContext.getAuthentication();
        final String username = authentication.getName();
        final Collection<? extends GrantedAuthority> userAuthorities = authentication.getAuthorities();
        if (Stream.of(requiredPermission.authorities()).noneMatch(
                authorityName -> userAuthorities.stream().anyMatch(userAuthority ->
                authorityName.equals(userAuthority.getAuthority()))))
        {
            log.error("User {} does not have the correct authorities required by endpoint", username);
            throw new BizException(SystemResponse.SAU0401, "HAHA");
        }
    }

    // ========= util method
    private void setUp(AppContext appContext, JoinPoint joinPoint, RequiredPermission requiredPermission) {
        Object[] methodArgs = joinPoint.getArgs();
        if (methodArgs.length > 0) {
            appContext.getRequestContext().setMethodArguments(joinPoint.getArgs());
        }
        appContext.getRequestContext().setEndDt(InstantUtil.parse_tz(Instant.now(), ZoneOffset.UTC));
    }
}
