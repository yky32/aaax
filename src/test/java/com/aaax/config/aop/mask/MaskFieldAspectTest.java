package com.aaax.config.aop.mask;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaskFieldAspectTest {

    @Test
    @DisplayName("maskString should leave short input unchanged")
    void maskString_shouldLeaveShortInput() {
        assertEquals("ab", MaskFieldAspect.maskString("ab", 5, '*'));
    }

    @Test
    @DisplayName("maskString should mask leading digits")
    void maskString_shouldMaskLeading() {
        assertEquals("***4567", MaskFieldAspect.maskString("1234567", 3, '*'));
    }
}
