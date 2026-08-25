package com.aaax.core.constant.enu;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;

import java.util.Arrays;

public enum LoginType {
    USERNAME, // simply just a username
    MOBILE,
    EMAIL,
    GOOGLE,
    FACEBOOK,
    APPLE,
    LINE,
    GRANDPAY, // reserved (legacy SSO id)
    OTP //mobile
    ;

    public static LoginType get(String input) {
        for (LoginType value : LoginType.values()) {
            if (    input.equalsIgnoreCase(value.name()) ||
                    input.equalsIgnoreCase(value.name().toLowerCase())){
                return value;
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(LoginType.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
