package com.aaax.config.aop.log;

import com.aaax.config.aop.BaseAspect;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;


/**
 * Core-logic for produce the log.
 */
@Aspect
@Slf4j
public class AuditLogAspect extends BaseAspect {

    @Before("@annotation(AuditLog)")
    public void auditLog_actionForBefore(JoinPoint joinPoint) {
        this.actionForBefore("AuditLog", joinPoint);
    }

    @Override
    public void executeForBefore(JoinPoint joinPoint) {
        super.executeForBefore(joinPoint);
    }

    @AfterReturning(pointcut = "@annotation(AuditLog)", returning = "returnValue")
    public void auditLog_actionForAfter(JoinPoint joinPoint, Object returnValue) {
        this.actionForAfter("AuditLog", joinPoint, returnValue);
    }

    @Override
    public void executeForAfter(JoinPoint joinPoint, Object returnValue) {
        super.executeForAfter(joinPoint, returnValue);
    }

    @AfterThrowing(pointcut = "@annotation(AuditLog)", throwing = "exception")
    public void auditLog_actionForAfter(JoinPoint joinPoint, Exception exception) {
        this.actionForAfter("AuditLog", joinPoint, exception);
    }

}
