package com.aaax.config.aop;

import com.aaax.core.common.AppContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;


/**
 * Core-logic for produce the log.
 */
@Slf4j
public class BaseAspect {

    public void actionForBefore(String annotationName, JoinPoint joinPoint) {
        log.info("-- [@{}] actionForBefore- class: {}, method: {}", annotationName, joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName());
        this.executeForBefore(joinPoint);
        log.info("-- [@{}] actionForBefore end - class: {}, method: {}", annotationName, joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName());
    }
    public void executeForBefore(JoinPoint joinPoint) {
        log.info("-- [executeForBefore] {}", joinPoint);
    }



    // =================
    public void actionForAfter(String annotationName, JoinPoint joinPoint) {
        log.info("-- [@{}] - class: {}, method: {}", annotationName, joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName());
        this.executeForAfter(joinPoint);
        log.info("-- [@{}] end - class: {}, method: {}", annotationName, joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName());
        AppContextHolder.clearContext();
    }

    public void actionForAfter(String annotationName, JoinPoint joinPoint, Object value) {
        log.info("-- [@{}] - class: {}, method: {}", annotationName, joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName());
        this.executeForAfter(joinPoint, value);
        log.info("-- [@{}] end - class: {}, method: {}", annotationName, joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName());
        AppContextHolder.clearContext();
    }
    public void executeForAfter(JoinPoint joinPoint) {
        log.info("-- [executeForAfter] {}", joinPoint);
    }

    public void executeForAfter(JoinPoint joinPoint, Object value) {
        log.info("-- [executeForAfter] {}, {}", joinPoint, value);
    }

}
