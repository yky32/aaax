package com.aaax.core.constant.enu;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;

import java.util.Arrays;

public enum Device {
    MOBILE,
    SERVER,
    WEB,
    PC
    ;

    public static Device get(String input) {
        for (Device value : Device.values()) {
            if (input.equalsIgnoreCase(value.name())){
                return value;
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(Device.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
