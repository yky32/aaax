package com.aaax.core.constant.enu;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;

import java.util.Arrays;

/**
 * SCHEDULED - Future
 * REALTIME - REALTIME
 */
public enum NotificationAction {
    SCHEDULED,
    REALTIME,
    ;

    public static NotificationAction get(String input) {
        for (NotificationAction value : NotificationAction.values()) {
            if (input.equalsIgnoreCase(value.name())){
                return value;
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(NotificationAction.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
