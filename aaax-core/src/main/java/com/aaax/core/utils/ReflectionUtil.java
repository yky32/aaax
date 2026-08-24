package com.aaax.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;

import java.lang.reflect.Method;

@Slf4j
public class ReflectionUtil {

    public static Method getMethod(JoinPoint joinPoint) {
        Method method = null;
        try {
            method = joinPoint.getTarget().getClass().getMethod(
                    joinPoint.getSignature().getName(),
                    getParameterTypes(joinPoint.getArgs())
            );
        } catch (NoSuchMethodException e) {
            log.error("--- getMethod dead in logAspect => {}", e.getMessage());
            e.printStackTrace();
        }
        return method;
    }

    public static Class<?>[] getParameterTypes(Object[] args) {
        Class<?>[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass();
        }
        return parameterTypes;
    }
}
