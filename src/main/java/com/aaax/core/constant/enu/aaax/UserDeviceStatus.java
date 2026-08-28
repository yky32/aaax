package com.aaax.core.constant.enu.aaax;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;

import java.util.Arrays;

public enum UserDeviceStatus {
    ACTIVE,
    INACTIVE
    ;


    public static UserDeviceStatus get(String input) {
        for (UserDeviceStatus value : UserDeviceStatus.values()) {
            if (input.equalsIgnoreCase(value.name())) {
                return value;
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(UserDeviceStatus.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }

}