package com.aaax.core.constant.enu;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;

import java.util.Arrays;

public enum DateTimeUnit {
    HOUR,
    DAY,
    WEEK,
    MONTH;

    public static DateTimeUnit get(String input) {
        for (DateTimeUnit value : DateTimeUnit.values()) {
            if (input.equalsIgnoreCase(value.name()) || input.equalsIgnoreCase(value.name().toLowerCase())) {
                return value;
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(DateTimeUnit.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
