package com.aaax.server.usecase.metrics;

import com.aaax.server.entity.dto.response.GetDailyLoginsMetricsResponseDto;
import com.aaax.server.repository.AuthenticationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class QueryDailyLoginsMetricsUseCase {

    static final Set<String> EXCLUDED_EVENTS = Set.of("refresh_token", "login-attempts");

    private final AuthenticationLogRepository authenticationLogRepository;

    @Value("${aaax.config.microservice.timezone:UTC}")
    private String timezone;

    public GetDailyLoginsMetricsResponseDto execute() {
        Instant now = Instant.now();
        Instant currentWindowStart = now.minus(24, ChronoUnit.HOURS);
        Instant previousWindowStart = now.minus(48, ChronoUnit.HOURS);

        long value = authenticationLogRepository.countDistinctUsersBetween(
                currentWindowStart, now, EXCLUDED_EVENTS);
        long previous = authenticationLogRepository.countDistinctUsersBetween(
                previousWindowStart, currentWindowStart, EXCLUDED_EVENTS);
        double trendPct = previous == 0
                ? (value > 0 ? 100d : 0d)
                : ((value - previous) * 100.0 / previous);

        return GetDailyLoginsMetricsResponseDto.builder()
                .value(value)
                .trendPct(trendPct)
                .series7d(buildSeries7d(now))
                .build();
    }

    private List<Double> buildSeries7d(Instant now) {
        ZoneId zone = ZoneId.of(timezone);
        LocalDate today = LocalDate.ofInstant(now, zone);
        LocalDate startDay = today.minusDays(6);
        Instant fromDt = startDay.atStartOfDay(zone).toInstant();
        Instant toDt = today.plusDays(1).atStartOfDay(zone).toInstant();

        Map<LocalDate, Long> byDay = new HashMap<>();
        List<Object[]> rows = authenticationLogRepository.countDistinctUsersGroupedByDay(
                fromDt, toDt, timezone, EXCLUDED_EVENTS);
        for (Object[] row : rows) {
            LocalDate day = toLocalDate(row[0]);
            long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
            if (day != null) {
                byDay.put(day, count);
            }
        }

        List<Double> series = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate day = startDay.plusDays(i);
            series.add(byDay.getOrDefault(day, 0L).doubleValue());
        }
        return series;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.util.Date utilDate) {
            return Instant.ofEpochMilli(utilDate.getTime()).atZone(ZoneId.of("UTC")).toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }
}
