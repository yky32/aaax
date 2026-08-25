package com.aaax.server.validation;

import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UaaValidationTest {

    @ParameterizedTest
    @CsvSource({
            "91234567, MOBILE",
            "user@example.com, EMAIL",
            "plainuser, USERNAME"
    })
    @DisplayName("detechLoginType should classify identifiers")
    void detechLoginType_shouldClassify(String input, LoginType expected) {
        assertEquals(expected, UaaValidation.detechLoginType(input));
    }

    @Test
    @DisplayName("detechLoginType should reject Chinese characters")
    void detechLoginType_shouldRejectChinese() {
        assertThrows(BizException.class, () -> UaaValidation.detechLoginType("用户@test.com"));
    }

    @Test
    @DisplayName("detechLoginType should reject multiple @ characters")
    void detechLoginType_shouldRejectMultipleAt() {
        assertThrows(BizException.class, () -> UaaValidation.detechLoginType("a@b@c.com"));
    }

    @Test
    @DisplayName("check_passwordRequirement should encode when all regexps match")
    void checkPasswordRequirement_shouldEncodeWhenValid() {
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode("Password1")).thenReturn("encoded");

        String result = UaaValidation.check_passwordRequirement(
                encoder, "Password1", List.of(".*[A-Z].*", ".*[0-9].*"));

        assertEquals("encoded", result);
        verify(encoder).encode("Password1");
    }

    @Test
    @DisplayName("check_passwordRequirement should throw when regexp fails")
    void checkPasswordRequirement_shouldThrowWhenInvalid() {
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        assertThrows(BizException.class, () ->
                UaaValidation.check_passwordRequirement(encoder, "weak", List.of(".*[A-Z].*")));
        verifyNoInteractions(encoder);
    }
}
