package com.aaax.core.constant.enu;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;

import java.util.Arrays;

public enum UserStatus {
    ACTIVE,
    PENDING_VERIFY,
    SUSPENDED,
    INACTIVE
    ;

    public static UserStatus get(String input) {
        for (UserStatus value : UserStatus.values()) {
            if (input.equals(value.name())) {
                return value;
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(UserStatus.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
