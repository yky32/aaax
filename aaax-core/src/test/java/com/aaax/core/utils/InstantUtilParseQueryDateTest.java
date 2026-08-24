package com.aaax.core.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstantUtilParseQueryDateTest {

    @Test
    void parseStartDt_blankUsesEarliest() {
        Instant expected = InstantUtil.parse(InstantUtil.EARLIEST_DATE);
        assertEquals(expected, InstantUtil.parseStartDt(null));
        assertEquals(expected, InstantUtil.parseStartDt(""));
        assertEquals(expected, InstantUtil.parseStartDt("   "));
    }

    @Test
    void parseStartDt_isUtcMidnight() {
        assertEquals(
                LocalDate.parse("2026-07-18").atStartOfDay(ZoneOffset.UTC).toInstant(),
                InstantUtil.parseStartDt("2026-07-18"));
    }

    @Test
    void parseEndDtInclusive_blankUsesNeverExpired() {
        Instant expected = InstantUtil.parse(InstantUtil.NEVER_EXPIRED);
        assertEquals(expected, InstantUtil.parseEndDtInclusive(null));
        assertEquals(expected, InstantUtil.parseEndDtInclusive(""));
    }

    @Test
    void parseEndDtInclusive_coversFullCalendarDay() {
        Instant end = InstantUtil.parseEndDtInclusive("2026-07-18");
        Instant lastSecondOfDay = LocalDate.parse("2026-07-18")
                .atTime(23, 59, 59)
                .toInstant(ZoneOffset.UTC);
        Instant nextMidnight = LocalDate.parse("2026-07-19")
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        assertTrue(!end.isBefore(lastSecondOfDay));
        assertTrue(end.isBefore(nextMidnight));
        assertEquals(nextMidnight.minusNanos(1), end);
    }
}
