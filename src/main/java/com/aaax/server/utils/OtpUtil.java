package com.aaax.server.utils;

import com.aaax.core.constant.RegexPatternConstant;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;
import com.aaax.core.utils.StringUtil;
import com.aaax.core.utils.ValidationUtil;
import com.aaax.server.config.redis.RedisKey;
import com.aaax.server.entity.enu.OtpType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OtpUtil {

    public static void isValidKey(RedisKey usecase) {
        if (!usecase.getDomain().equals(RedisKey.OTP_GENERAL.getDomain())) {
            throw new BizException(SystemResponse.PAM0400, "Invalid OTP usecase.".concat(usecase.getKey()));
        }
    }

    public static String generate(int digits, OtpType otpType) {
        switch (otpType) {
            case DIGIT -> {
                return StringUtil.randomDigit(digits);
            }
            case LETTER -> {
                return StringUtil.randomLetters(digits);
            }
            case ALPHANUMERIC -> {
                return StringUtil.randomAlphanumeric(digits);
            }
        }
        throw new BizException(SystemResponse.PAM0400, "Invalid OtpType =>".concat(otpType.name()));
    }

    public static void isValidRecipient(String to) {
        Map message = new HashMap<>();
        message.put("to", to);
        ValidationUtil.nonEmptyNonNull_regexps(
                to, "to",
                List.of(RegexPatternConstant.EMAIL_PATTERN, RegexPatternConstant.PHONE_WITH_AREA_CODE_PATTERN),
                message
        );
    }

    public static String markAsGenerated(String redisKey) {
        return markAs(redisKey, ":generated");
    }

    public static String markAsVerified(String redisKey) {
        return markAs(redisKey, ":isVerified");
    }

    public static String markAs(String redisKey, String action) {
        return redisKey.concat(action);
    }
}
