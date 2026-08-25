package com.aaax.core.constant.enu;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;

import java.util.Arrays;

public enum NotificationExecutionStatus {
    PENDING,
    EXECUTED,
    CANCELLED,
    FAILED,
    ARCHIVED,
    ALL
    ;

    public static NotificationExecutionStatus get(String input) {
        for (NotificationExecutionStatus value : NotificationExecutionStatus.values()) {
            if (input.equals(value.name())) {
                return value;
            }
        }
        String message = String.format("Wrong %s value. [" + input + "] not in->" + Arrays.asList(NotificationExecutionStatus.values()), NotificationExecutionStatus.class.getName());
        throw new BizException(SystemResponse.PAM0400, message);
    }

}