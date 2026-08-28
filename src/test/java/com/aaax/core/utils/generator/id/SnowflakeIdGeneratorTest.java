package com.aaax.core.utils.generator.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeIdGeneratorTest {

    @Test
    @DisplayName("sequence cap is 12-bit shift, not XOR (2 ^ 12 == 14)")
    void maxSequence_isTwelveBitShiftNotXor() {
        assertEquals(14, 2 ^ 12);
        assertEquals(4096L, SnowflakeIdGenerator.MAX_SEQUENCE);
        assertTrue(SnowflakeIdGenerator.MAX_SEQUENCE > 14);
    }
}
