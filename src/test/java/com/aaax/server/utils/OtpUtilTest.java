package com.aaax.server.utils;

import com.aaax.core.exception.BizException;
import com.aaax.server.config.redis.RedisKey;
import com.aaax.server.entity.enu.OtpType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class OtpUtilTest {

    @Test
    @DisplayName("isValidKey should accept OTP domain keys")
    void isValidKey_shouldAcceptOtpDomain() {
        assertDoesNotThrow(() -> OtpUtil.isValidKey(RedisKey.OTP_GENERAL));
        assertDoesNotThrow(() -> OtpUtil.isValidKey(RedisKey.OTP_USER_REGISTER));
        assertDoesNotThrow(() -> OtpUtil.isValidKey(RedisKey.OTP_RESET_PASSWORD));
    }

    @Test
    @DisplayName("isValidKey should reject non-OTP domain keys")
    void isValidKey_shouldRejectNonOtpDomain() {
        assertThrows(BizException.class, () -> OtpUtil.isValidKey(RedisKey.USER_OAUTH_TOKENS));
        assertThrows(BizException.class, () -> OtpUtil.isValidKey(RedisKey.DEVICE_SESSIONS));
    }

    @ParameterizedTest
    @EnumSource(OtpType.class)
    @DisplayName("generate should produce code of requested length for each OtpType")
    void generate_shouldProduceCodeOfRequestedLength(OtpType otpType) {
        String code = OtpUtil.generate(6, otpType);

        assertNotNull(code);
        assertEquals(6, code.length());
    }

    @Test
    @DisplayName("isValidRecipient should accept email")
    void isValidRecipient_shouldAcceptEmail() {
        assertDoesNotThrow(() -> OtpUtil.isValidRecipient("user@example.com"));
    }

    @Test
    @DisplayName("isValidRecipient should accept phone with area code")
    void isValidRecipient_shouldAcceptPhone() {
        assertDoesNotThrow(() -> OtpUtil.isValidRecipient("852-91234567"));
    }

    @Test
    @DisplayName("isValidRecipient should reject invalid recipient")
    void isValidRecipient_shouldRejectInvalid() {
        assertThrows(Exception.class, () -> OtpUtil.isValidRecipient("not-valid"));
    }

    @Test
    @DisplayName("markAs helpers should append action suffixes")
    void markAs_shouldAppendSuffixes() {
        assertEquals("otp:key:generated", OtpUtil.markAsGenerated("otp:key"));
        assertEquals("otp:key:isVerified", OtpUtil.markAsVerified("otp:key"));
        assertEquals("otp:key:custom", OtpUtil.markAs("otp:key", ":custom"));
    }
}
