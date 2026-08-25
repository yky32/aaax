package com.aaax.core.utils;

import com.aaax.core.constant.enu.Locale;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Central locale parsing for notification producers and shared profile context.
 * Accepts enum name ({@code EN}), BCP-47 code ({@code zh-TW}), or {@link Locale#getI18nCode()} ({@code zh}).
 */
public final class NotificationLocaleUtil {

    private NotificationLocaleUtil() {
    }

    public static Locale parse(String raw) {
        if (StringUtils.isBlank(raw)) {
            return Locale.EN;
        }
        return Locale.get(raw.trim());
    }

    /** Canonical {@link Locale#getI18nCode()} for Redis / API storage. */
    public static String toI18nCode(String raw) {
        return parse(raw).getI18nCode();
    }

    /** Outbound {@code userLocal} from a stored locale token. */
    public static Locale userLocalFromRaw(String raw) {
        return parse(raw);
    }

    /** Kafka {@code locale} list: EN first, then user locale when different. */
    public static List<Locale> localesWithFallback(Locale userLocal) {
        Locale resolved = userLocal != null ? userLocal : Locale.EN;
        Set<Locale> locales = new LinkedHashSet<>();
        locales.add(Locale.EN);
        if (resolved != Locale.EN) {
            locales.add(resolved);
        }
        return new ArrayList<>(locales);
    }
}
