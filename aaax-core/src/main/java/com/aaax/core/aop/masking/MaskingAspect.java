package com.aaax.core.aop.masking;

import com.aaax.core.aop.BaseAspect;
import com.aaax.core.response.Result;
import com.aaax.core.utils.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;


/**
 * Core-logic for produce the log.
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class MaskingAspect extends BaseAspect {

    @Value("${spring.application.name}")
    private String serviceName;

    //@AfterReturning(pointcut = "execution(* com.aaax.payment.endpoint.enquiry.PaymentQueryEndpoint.getAll(..))", returning = "result")
    @AfterReturning(value = "@annotation(EnableMasking)", returning = "result")
    public void activityLog_actionForAfter(JoinPoint joinPoint, Object result) {
        super.actionForAfter(this.getClass().getSimpleName(), joinPoint, result);
    }

    @SneakyThrows
    @Override
    public void executeForAfter(JoinPoint joinPoint, Object result) {
        if (result == null) {
            return;
        }
        // Check if the result is of type ResponseClass
        if (result instanceof Result<?> response) {
            Object data = response.getData();
            if (data instanceof Map) {
                process((Map<?, ?>) data);
            } else {
                maskSensitiveFields(data);
            }
        } else if (result instanceof Map) {
            process((Map<?, ?>) result);
        }
    }

    private void process(Map<?, ?> result) {
        // Iterate over the key set
        Map<?, ?> map = result;
        for (Object key : map.keySet()) {
            Map value = (Map) map.get(key);
            for (Object _key : value.keySet()) {
                Object object = value.get(_key);
                if (!(object instanceof String)) {
                    maskSensitiveFields(object);
                }
            }
        }
    }

    private void maskSensitiveFields(Object obj) {
        if (obj instanceof List<?> list) {
            for (Object item : list) {
                inspectFields(item);
            }
        } else {
            inspectFields(obj);
        }
    }

    @SneakyThrows
    private void inspectFields(Object result) {
        if (result != null) {
            Class<?> objClass = result.getClass();
            Field[] fields = objClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Masked annotation = field.getAnnotation(Masked.class);
                Object value = field.get(result);
                //======================= start inspection ==============

                if (field.isAnnotationPresent(Masked.class)) {
                    if (value == null) {
                        continue;
                    }
                    if (value instanceof Map<?, ?>) {
                        inspectMap(annotation, (Map<?, ?>) value);
                        continue;
                    }
                    // === check its object.
                    String originalValue = (String) value;
                    String maskedValue = this.mask(originalValue, annotation);
                    field.set(result, maskedValue);
                    continue;
                }
                // === Check nested objects
                // === By-pass some unrelated-fields
                if (isBypassType_forNestedLoopChecking(field.getType())) {
                    continue;
                }
                // ============ Take Action.
                if (value instanceof List<?> list) {
                    for (Object item : list) {
                        if (isBypassType_forNestedLoopChecking(item.getClass())) {
                            continue;
                        }
                        maskSensitiveFields(item);
                    }
                } else if (value instanceof Map) {
                    // default dont break the logic.
                } else {
                    if (value != null) {
                        inspectFields(value); // Recursively inspect nested fields
                    }
                }
                //======================= end inspection  ==============
            }
        }
    }

    private @Nullable String mask(String originalValue, Masked annotation) {
        return toExecuteMaskingValue(
                originalValue,
                annotation.start(),
                annotation.end() > 0 ? annotation.end() : originalValue.length(),
                annotation.symbol()
        );
    }

    private void inspectMap(Masked annotation, Map<?,?> value) {
        String[] maskedFields = annotation.fields();
        for (String maskedField : maskedFields) {
            for (Map.Entry<?, ?> entry : value.entrySet()) {
                String key = (String) entry.getKey();
                Object _v = entry.getValue();
                if (_v instanceof Map) {
                    inspectMap(annotation, (Map<?, ?>) entry.getValue()); // === check nested maps.
                }
                // ==== check
                if (key.equalsIgnoreCase(maskedField)) {
                    if (_v instanceof String) {
                        ((Map<String, Object >) value).put(key, toExecuteMaskingValue(
                                (String) _v,
                                annotation.start(),
                                annotation.end() > 0 ? annotation.end() : ((String) _v).length(),
                                annotation.symbol()
                        ));
                    }
                }
            }
        }
    }

    private static boolean isBypassType_forNestedLoopChecking(Class<?> type) {
        return
                Long.class.equals(type) ||
                String.class.equals(type) ||
                Integer.class.equals(type) ||
                type.isEnum() ||
                Instant.class.equals(type) ||
                Boolean.TYPE.equals(type) ||
                Boolean.class.equals(type)
        ;
    }

    private String toExecuteMaskingValue(String value, int start, int end, char symbol) {
        if (value != null && !value.isEmpty()) {
            return StringUtil.maskString(value, start, end, symbol);
        }
        return value;
    }
}
