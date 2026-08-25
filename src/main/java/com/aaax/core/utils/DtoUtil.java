package com.aaax.core.utils;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

public class DtoUtil {

    @SneakyThrows
    public static Object partialUpdateBuilder(Object putObject, Object targetObject) {
        Object _object = putObject;
        Field[] fields = _object.getClass().getDeclaredFields();
        return processFields(fields, _object, targetObject);
    }


    @NotNull
    public static <T, F> Map<T, F> partialUpdateBuilderToMap(Object putObject, Object targetObject) {
        Map<String, Object> putMap = JSONUtil.convertFromObject(putObject, Map.class);
        Map<String, Object> targetMap = JSONUtil.convertFromObject(targetObject, Map.class);

        // Compare and update values for the same keys
        for (String _key : targetMap.keySet()) {
            if (putMap.containsKey(_key)) {
                targetMap.put(_key, putMap.get(_key)); // doReplace
            }
        }
        // Add new key becoz not the same
        for (String _key : putMap.keySet()) {
            if (!targetMap.containsKey(_key)) {
                targetMap.put(_key, putMap.get(_key));
            }
        }
        return (Map<T, F>) targetMap;
    }


    public static <T, F> Map<T, F> nestedUpdateBuilderToMap(Object putObject, Object targetObject) {
        Map<String, Object> putMap = JSONUtil.convertFromObject(putObject, Map.class);
        Map<String, Object> targetMap = JSONUtil.convertFromObject(targetObject, Map.class);
        nested(targetMap, putMap);
        return (Map<T, F>) targetMap;
    }

    private static void nested(Map<String, Object> targetMap, Map<String, Object> putMap) {
        // Compare and update values for the same keys
        for (String _key : targetMap.keySet()) {
            if (putMap.containsKey(_key)) {
                // check the target map is a nested map.
                Object targetObject = targetMap.get(_key);
                if (targetObject instanceof Map<?,?>) {
                    nested((Map<String, Object>) targetObject, (Map<String, Object>) putMap.get(_key));
                } else {
                    targetMap.put(_key, putMap.get(_key)); // doReplace
                }
            }
        }
        // Add new key becoz not the same
        for (String _key : putMap.keySet()) {
            if (!targetMap.containsKey(_key)) {
                targetMap.put(_key, putMap.get(_key));
            }
        }
    }



    @SneakyThrows
    public static Object processFields(Field[] fields, Object _object, Object targetObject) {
        for (Field f : fields) {
            f.setAccessible(true);
            Object value = f.get(_object);
            if (Objects.nonNull(value)) {
                f.set(targetObject, value);
            }
        }
        return targetObject;
    }
}
