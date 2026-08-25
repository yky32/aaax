package com.aaax.core.utils;

import com.aaax.core.constant.enu.Locale;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationLocaleUtilTest {

    @Test
    void parse_blankDefaultsToEn() {
        assertEquals(Locale.EN, NotificationLocaleUtil.parse(null));
        assertEquals(Locale.EN, NotificationLocaleUtil.parse(" "));
    }

    @Test
    void parse_acceptsNameCodeAndI18nCode() {
        assertEquals(Locale.EN, NotificationLocaleUtil.parse("EN"));
        assertEquals(Locale.EN, NotificationLocaleUtil.parse("en"));
        assertEquals(Locale.zh_TW, NotificationLocaleUtil.parse("zh"));
        assertEquals(Locale.zh_TW, NotificationLocaleUtil.parse("zh-TW"));
        assertEquals(Locale.zh_TW, NotificationLocaleUtil.parse("zh_TW"));
    }

    @Test
    void toI18nCode_normalizesToCanonicalToken() {
        assertEquals("en", NotificationLocaleUtil.toI18nCode("EN"));
        assertEquals("zh", NotificationLocaleUtil.toI18nCode("zh-TW"));
    }

    @Test
    void localesWithFallback_includesEnAndUserLocale() {
        assertEquals(List.of(Locale.EN), NotificationLocaleUtil.localesWithFallback(Locale.EN));
        assertEquals(List.of(Locale.EN, Locale.zh_TW), NotificationLocaleUtil.localesWithFallback(Locale.zh_TW));
        assertEquals(List.of(Locale.EN), NotificationLocaleUtil.localesWithFallback(null));
    }
}
