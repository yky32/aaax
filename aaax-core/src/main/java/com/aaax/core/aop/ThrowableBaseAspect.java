package com.aaax.core.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;


/**
 * Core-logic for produce the log.
 */
@Slf4j
public class ThrowableBaseAspect {

    public void actionForBefore(String annotationName, JoinPoint joinPoint) {
        try {
            log.info("-- [@{}] actionForBefore- class: {}, method: {}", annotationName, joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName());
            this.executeForBefore(joinPoint);
            log.info("-- [@{}] actionForBefore end - class: {}, method: {}", annotationName, joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName());
        } catch (Throwable exception) {
            log.error("-- [@{}] Error in [actionForBefore] end - class: {}, method: {}", annotationName, joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName());
            throw exception;
        }
    }
    public void executeForBefore(JoinPoint joinPoint) {
        log.info("-- [executeForBefore] {}", joinPoint);
    }



    // =================
    public void actionForAfter(String annotationName, JoinPoint joinPoint) {
        try {
            log.info("-- [@{}] - class: {}, method: {}", annotationName, joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName());
            this.executeForAfter(joinPoint);
            log.info("-- [@{}] end - class: {}, method: {}", annotationName, joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName());
        } catch (Exception exception) {
            log.error("-- [@{}] Error in [actionForAfter] end - class: {}, method: {}", annotationName, joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName());
            throw exception;
        }
    }

    public void actionForAfter(String annotationName, JoinPoint joinPoint, Object value) {
        try {
            log.info("-- [@{}] - class: {}, method: {}", annotationName, joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName());
            this.executeForAfter(joinPoint, value);
            log.info("-- [@{}] end - class: {}, method: {}", annotationName, joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName());
        } catch (Exception exception) {
            log.error("-- [@{}] Error in [actionForAfter] end - class: {}, method: {}", annotationName, joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName());
            throw exception;
        }
    }
    public void executeForAfter(JoinPoint joinPoint) {
        log.info("-- [executeForAfter] {}", joinPoint);
    }

    public void executeForAfter(JoinPoint joinPoint, Object value) {
        log.info("-- [executeForAfter] {}, {}", joinPoint, value);
    }

}
