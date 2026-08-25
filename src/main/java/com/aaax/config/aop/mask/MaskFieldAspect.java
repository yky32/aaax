package com.aaax.config.aop.mask;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

//@Aspect
//@Component
public class MaskFieldAspect {
    @Around("execution(* com.*.*.endpoint..*(..))")
    public Object maskField(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        Class<?> clazz = result.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            // __ jump in Result.data  ==> we all aligned the result in this field [data]
            if (field.getName().equalsIgnoreCase("data")) {
                field.setAccessible(true);
                Object value = field.get(result);
                maskFields(value);
            }
        }
        return result;
    }

    private void maskFields(Object object) throws IllegalAccessException {

        if (object == null) {
            return;
        }

        Class<?> clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {

            List<?> listValue = (List<?>) field.get(object);
            if (listValue != null) {
                System.out.println(field.getName() + " is a List: " + listValue);
            }
            if (List.class.isAssignableFrom(field.getType())) {

            }

            if (    field.getType().equals(String.class) ||
                    field.getType().equals(Instant.class) ||
                    field.getType().equals(Integer.class) ||
                    field.getType().equals(BigDecimal.class) ||
                    Enum.class.isAssignableFrom(field.getType())
            ) {
                field.setAccessible(true);
                Object value = field.get(object);
                if (field.isAnnotationPresent(MaskField.class)) {
                    field.set(object, maskValue((String) value, field));
                }
            } else {
                field.setAccessible(true);
                Object value = field.get(object);
                maskFields(value);
            }
        }
    }

    private String maskValue(String value, Field field) {
        MaskField maskField = AnnotationUtils.getAnnotation(field, MaskField.class);
        Objects.requireNonNull(maskField);
        switch (maskField.type()) {
            case PAN, HKID, OTHER -> {
                return maskString(value, maskField.numDigitsToMask(), maskField.symbol());
            }
            default -> throw new BizException(SystemResponse.PAM0400, Map.of("maskField", maskField));
        }
    }

    public static String maskString(String input, int numDigitsToMask, char maskCharacter) {
        int length = input.length();
        if (length <= numDigitsToMask) {
            return input;
        } else {
            String maskedPart = input.substring(0, numDigitsToMask).replaceAll(".", String.valueOf(maskCharacter));
            String remainingPart = input.substring(numDigitsToMask);
            return maskedPart + remainingPart;
        }
    }
}
