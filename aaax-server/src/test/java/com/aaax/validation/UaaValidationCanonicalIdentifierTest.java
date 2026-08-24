package com.aaax.validation;

import com.aaax.core.constant.enu.LoginType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UaaValidationCanonicalIdentifierTest {

    @Test
    @DisplayName("email is lowercased")
    void email_lower() {
        assertEquals("acekaiyin@gmail.com", UaaValidation.toCanonicalIdentifier("Acekaiyin@gmail.com"));
        assertEquals("admin@tgt.gg", UaaValidation.toCanonicalIdentifier("ADmin@tgt.gg"));
        assertEquals(LoginType.EMAIL, UaaValidation.detechLoginType("Acekaiyin@gmail.com"));
    }

    @Test
    @DisplayName("email trim + lower")
    void email_trim() {
        assertEquals("acekaiyin@gmail.com", UaaValidation.toCanonicalIdentifier("  AceKaiyin@Gmail.COM  "));
    }

    @Test
    @DisplayName("mobile not lowercased")
    void mobile_unchanged() {
        assertEquals("91234567", UaaValidation.toCanonicalIdentifier("91234567"));
    }
}
