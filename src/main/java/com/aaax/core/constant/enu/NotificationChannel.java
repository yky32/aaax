package com.aaax.core.constant.enu;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;

import java.util.Arrays;

/**
 * SMS - Text SMS (csl, smartone, 3hk)
 * EMAIL - email
 * APP_PUSH - Firebase
 * IN_APP_PUSH - Only trigger inside the app push toast (ui effect), example FUTU Trading App.
 */
public enum NotificationChannel {
    SMS,
    EMAIL,
    APP_PUSH,
    IN_APP_PUSH,
    WEB_PUSH
    ;

    public static NotificationChannel get(String input) {
        for (NotificationChannel value : NotificationChannel.values()) {
            if (input.equalsIgnoreCase(value.name())){
                return value;
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(NotificationChannel.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
