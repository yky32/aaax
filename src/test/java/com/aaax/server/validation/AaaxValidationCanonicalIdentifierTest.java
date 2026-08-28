package com.aaax.server.validation;

import com.aaax.core.constant.enu.LoginType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AaaxValidationCanonicalIdentifierTest {

    @Test
    @DisplayName("email is lowercased")
    void email_lower() {
        assertEquals("acekaiyin@gmail.com", AaaxValidation.toCanonicalIdentifier("Acekaiyin@gmail.com"));
        assertEquals("admin@aaax.local", AaaxValidation.toCanonicalIdentifier("ADmin@aaax.local"));
        assertEquals(LoginType.EMAIL, AaaxValidation.detechLoginType("Acekaiyin@gmail.com"));
    }

    @Test
    @DisplayName("email trim + lower")
    void email_trim() {
        assertEquals("acekaiyin@gmail.com", AaaxValidation.toCanonicalIdentifier("  AceKaiyin@Gmail.COM  "));
    }

    @Test
    @DisplayName("mobile not lowercased")
    void mobile_unchanged() {
        assertEquals("91234567", AaaxValidation.toCanonicalIdentifier("91234567"));
    }
}
