package com.aaax.core.entity.dto.util.enu;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;

import java.util.Arrays;

public enum RefDataType {
    COLLECTION,
    SINGLE,
    ALL // for filtering
    ;

    public static RefDataType get(String input) {
        for (RefDataType value : RefDataType.values()) {
            if (input.equalsIgnoreCase(value.name())) {
                return value;
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(RefDataType.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
    }
