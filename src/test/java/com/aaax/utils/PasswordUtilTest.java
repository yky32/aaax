package com.aaax.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    @DisplayName("generateCommonLangPassword should return non-blank shuffled password")
    void generateCommonLangPassword_shouldReturnNonBlank() {
        String password = PasswordUtil.generateCommonLangPassword();

        assertNotNull(password);
        assertFalse(password.isBlank());
        assertTrue(password.length() >= 24);
    }

    @Test
    @DisplayName("generateCommonLangPassword should produce varying results")
    void generateCommonLangPassword_shouldVary() {
        String first = PasswordUtil.generateCommonLangPassword();
        String second = PasswordUtil.generateCommonLangPassword();

        assertNotEquals(first, second);
    }
}
