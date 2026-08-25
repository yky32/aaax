package com.aaax.core.constant.enu;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;

import java.util.Arrays;

public enum NotificationFrequency {
    ONE_OFF,
    RECURRING,
    ALL;

    public static NotificationFrequency get(String input) {
        for (NotificationFrequency value : NotificationFrequency.values()) {
            if (input.equals(value.name())) {
                return value;
            }
        }
        String message = String.format("Wrong %s value. [" + input + "] not in->" + Arrays.asList(NotificationFrequency.values()), NotificationFrequency.class.getName());
        throw new BizException(SystemResponse.PAM0400, message);
    }

}