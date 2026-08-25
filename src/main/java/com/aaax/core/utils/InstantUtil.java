package com.aaax.core.utils;

import com.aaax.core.constant.enu.DateTimeUnit;
import com.aaax.core.entity.dto.util.response.StartDtEndDtResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Helpers for {@link Instant}: formatting, parsing, zone conversion, and simple calendar math.
 * Naming note: {@code parse(...)} is used both to format instants to strings and to parse strings
 * to instants (legacy overloads); kept for backward compatibility across modules.
 */
public class InstantUtil {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    public final static String STANDARD_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public final static String STANDARD_DATE_FORMAT_ISO = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public final static String EARLIEST_DATE = "1900-01-01 00:00:00";
    /** Sentinel "no expiry" end date (year 2099). */
    public final static String NEVER_EXPIRED = "2099-01-01 00:00:00";

    // -------------------------------------------------------------------------
    // List / query filters: optional yyyy-MM-dd → Instant bounds (UTC calendar day)
    // -------------------------------------------------------------------------

    /**
     * Parse optional list-filter {@code startDt} ({@code yyyy-MM-dd}) as the inclusive start
     * of that UTC calendar day (midnight UTC).
     * Blank or null → {@link #EARLIEST_DATE}.
     */
    public static Instant parseStartDt(String startDt) {
        if (StringUtils.isBlank(startDt)) {
            return parse(EARLIEST_DATE);
        }
        return LocalDate.parse(startDt.trim()).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * Parse optional list-filter {@code endDt} ({@code yyyy-MM-dd}) as the inclusive end
     * of that UTC calendar day (last nanosecond before the next midnight UTC).
     * Suitable for JPA {@code between(createDt, start, end)}.
     * Blank or null → {@link #NEVER_EXPIRED}.
     */
    public static Instant parseEndDtInclusive(String endDt) {
        if (StringUtils.isBlank(endDt)) {
            return parse(NEVER_EXPIRED);
        }
        return LocalDate.parse(endDt.trim())
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .minusNanos(1);
    }

    // -------------------------------------------------------------------------
    // Calendar day in a zone → UTC start/end instants for that local date
    // -------------------------------------------------------------------------

    public static StartDtEndDtResponseDto getStartDtAndEndDt_inUTC(String zoneId) {
        return getStartDtAndEndDt_inUTC(zoneId, LocalDate.now());
    }

    public static StartDtEndDtResponseDto getStartDtAndEndDt_inUTC(String zoneId, LocalDate now) {
        if (zoneId == null || StringUtils.isEmpty(zoneId)) {
            throw new BizException(SystemResponse.PAM0400, "[zoneId] is null");
        }
        ZoneId _zoneId = ZoneId.of(zoneId);
        LocalDateTime startDateJST = LocalDateTime.of(now, LocalTime.of(0, 0, 0));
        Instant _startDt = startDateJST.atZone(_zoneId).toInstant();
        LocalDateTime endDateJST = LocalDateTime.of(now, LocalTime.of(23, 59, 59));
        Instant _endDt = endDateJST.atZone(_zoneId).toInstant();
        return StartDtEndDtResponseDto.builder()
                .startDt(_startDt)
                .endDt(_endDt)
                .build();
    }

    // -------------------------------------------------------------------------
    // Expiry
    // -------------------------------------------------------------------------

    public static boolean isExpired(Instant expiryTime) {
        Instant currentTime = Instant.now();
        return currentTime.isAfter(expiryTime);
    }

    public static boolean isExpired(Instant expiryTime, Instant currentTime) {
        return currentTime.isAfter(expiryTime);
    }

    // -------------------------------------------------------------------------
    // Format: Instant → String (UTC, fixed offset, or IANA zone)
    // -------------------------------------------------------------------------

    /** Formats instant in UTC using the given pattern. */
    public static String parse(Instant datetime, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneOffset.UTC);
        return formatter.format(datetime);
    }

    /** Formats instant in UTC using {@link #STANDARD_DATE_FORMAT}. */
    public static String parse(Instant datetime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(STANDARD_DATE_FORMAT).withZone(ZoneOffset.UTC);
        return formatter.format(datetime);
    }

    /** Formats instant using {@link #STANDARD_DATE_FORMAT} in the given zone offset. */
    public static String parse_tz(Instant datetime, ZoneOffset tz) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(STANDARD_DATE_FORMAT).withZone(tz);
        return formatter.format(datetime);
    }

    /** Formats instant with a custom pattern in an IANA timezone (e.g. profile {@code "Asia/Tokyo"}). */
    public static String formatWithTimezone(Instant instant, String dateFormat, String timezone) {
        if (instant == null) {
            return null;
        }
        if (timezone == null || StringUtils.isEmpty(timezone)) {
            throw new BizException(SystemResponse.PAM0400, "[timezone] is null or empty");
        }
        if (dateFormat == null || StringUtils.isEmpty(dateFormat)) {
            throw new BizException(SystemResponse.PAM0400, "[dateFormat] is null or empty");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat).withZone(ZoneId.of(timezone));
        return formatter.format(instant);
    }

    /** Formats instant using pattern via {@link OffsetDateTime} at the given offset. */
    public static String parse_offset(Instant datetime, ZoneOffset tz, String pattern) {
        OffsetDateTime offsetDateTime = datetime.atOffset(tz);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return formatter.format(offsetDateTime);
    }

    // -------------------------------------------------------------------------
    // Parse: String → Instant (java.time; {@code parse_tz} overloads)
    // -------------------------------------------------------------------------

    private static String determineFormat(String dateString) {
        if (dateString.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            return STANDARD_DATE_FORMAT;
        } else if (dateString.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z")) {
            return STANDARD_DATE_FORMAT_ISO;
        } else {
            throw new BizException(SystemResponse.PAM0400, "Incorrect Date Format");
        }
    }

    public static Instant parse_tz(String dateString, String timezone) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(determineFormat(dateString)).withZone(ZoneOffset.UTC);
        Instant utcDate = Instant.from(formatter.parse(dateString));
        ZoneId zoneId = ZoneId.of(timezone);
        return utcDate.atZone(zoneId).toInstant();
    }

    public static Instant parse_tz(String dateString, ZoneOffset tz) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(determineFormat(dateString)).withZone(tz);
        return Instant.from(formatter.parse(dateString));
    }

    public static Instant parse_tz(String dateString, String pattern, String timezone) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneOffset.UTC);
        Instant utcDate = Instant.from(formatter.parse(dateString));
        ZoneId zoneId = ZoneId.of(timezone);
        return utcDate.atZone(zoneId).toInstant();
    }

    public static Instant parse_tz(String dateString, String pattern, ZoneOffset tz) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withZone(tz);
        return Instant.from(formatter.parse(dateString));
    }

    // -------------------------------------------------------------------------
    // Parse: String → Instant (legacy {@link SimpleDateFormat}, throws via {@link SneakyThrows})
    // -------------------------------------------------------------------------

    public static DateFormat getDateFormatter() {
        return getDateFormatter(STANDARD_DATE_FORMAT);
    }

    public static DateFormat getDateFormatter(String pattern) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        return formatter;
    }

    @SneakyThrows
    public static Instant parse(String dateInString) {
        return getDateFormatter().parse(dateInString).toInstant();
    }

    @SneakyThrows
    public static Instant parse(String dateInString, String pattern) {
        return getDateFormatter(pattern).parse(dateInString).toInstant();
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    /** Throws {@link BizException} if the string does not match the pattern. */
    public static void isValid(String dateString, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            formatter.parse(dateString);
        } catch (DateTimeParseException e) {
            Map<Object, Object> map = Map.of(
                    "message", "Invalid dateString",
                    "dateString", dateString,
                    "pattern", pattern
            );
            throw new BizException(SystemResponse.PAM0400, map);
        }
    }

    /** Non-throwing variant of {@link #isValid(String, String)}. */
    public static boolean _isValid(String dateString, String pattern) {
        if (StringUtils.isEmpty(dateString)) {
            return false;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            formatter.parse(dateString);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Zone: same instant, different local wall-clock / calendar truncation
    // -------------------------------------------------------------------------

    public static Instant convertToZonedDateTimeDefault(Instant instant, String timezoneString) {
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of(timezoneString))
                .withHour(0)
                .withMinute(0)
                .withSecond(0);
        return zonedDateTime.toInstant();
    }

    public static ZonedDateTime convertToZonedDateTime(Instant instant, String timezoneString) {
        return instant.atZone(ZoneId.of(timezoneString));
    }

    // -------------------------------------------------------------------------
    // Duration arithmetic ({@link DateTimeUnit#MONTH} uses 30-day approximation)
    // -------------------------------------------------------------------------

    public static Instant plus(Instant instant, Integer number, DateTimeUnit dt) {
        Duration duration;
        switch (dt) {
            // TODO assume month is 30 day only
            case MONTH -> duration = Duration.ofDays(number * 30L);
            case WEEK -> duration = Duration.ofDays(number * 7L);
            case HOUR -> duration = Duration.ofHours(number);
            default -> duration = Duration.ofDays(number);
        }
        return instant.plus(duration);
    }

    public static String deduceTimezoneString(String timezoneStr) {
        ZoneId zoneId = ZoneId.of(timezoneStr);
        ZonedDateTime zdt = ZonedDateTime.now(zoneId);
        String offset = zdt.getOffset().getId();
        return "Z".equals(offset) ? "+00:00" : offset;
    }
}
