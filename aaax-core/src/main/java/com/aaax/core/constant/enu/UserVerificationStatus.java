package com.aaax.core.constant.enu;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;

import java.util.Arrays;

public enum UserVerificationStatus {
    PENDING,
    PENDING_CALLBACK,
    INVALID_CALLBACK,
    REJECTED,
    VERIFIED,
    SUSPENDED,
    ;

    public static UserVerificationStatus get(String input) {
        for (UserVerificationStatus value : UserVerificationStatus.values()) {
            if (input.equals(value.name())) {
                return value;
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(UserVerificationStatus.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
