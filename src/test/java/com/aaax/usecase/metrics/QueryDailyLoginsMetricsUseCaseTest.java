package com.aaax.usecase.metrics;

import com.aaax.entity.dto.response.GetDailyLoginsMetricsResponseDto;
import com.aaax.repository.AuthenticationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryDailyLoginsMetricsUseCaseTest {

    @Mock
    private AuthenticationLogRepository authenticationLogRepository;

    @InjectMocks
    private QueryDailyLoginsMetricsUseCase useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "timezone", "UTC");
    }

    @Test
    @DisplayName("should compute value, trendPct, and fill series7d oldest to newest")
    void shouldComputeMetrics() {
        when(authenticationLogRepository.countDistinctUsersBetween(any(), any(), any()))
                .thenReturn(120L, 100L);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(authenticationLogRepository.countDistinctUsersGroupedByDay(any(), any(), eq("UTC"), any()))
                .thenReturn(List.of(
                        new Object[]{Date.valueOf(today.minusDays(6)), 10L},
                        new Object[]{Date.valueOf(today.minusDays(1)), 20L},
                        new Object[]{Date.valueOf(today), 30L}
                ));

        GetDailyLoginsMetricsResponseDto result = useCase.execute();

        assertEquals(120L, result.getValue());
        assertEquals(20d, result.getTrendPct());
        assertEquals(7, result.getSeries7d().size());
        assertEquals(10d, result.getSeries7d().get(0));
        assertEquals(0d, result.getSeries7d().get(1));
        assertEquals(20d, result.getSeries7d().get(5));
        assertEquals(30d, result.getSeries7d().get(6));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> excluded = ArgumentCaptor.forClass(Collection.class);
        verify(authenticationLogRepository, org.mockito.Mockito.times(2))
                .countDistinctUsersBetween(any(Instant.class), any(Instant.class), excluded.capture());
        assertTrue(excluded.getValue().containsAll(List.of("refresh-token", "login-attempts")));
    }

    @Test
    @DisplayName("should return 100 trendPct when previous window is zero and current has users")
    void shouldReturnFullTrendWhenPreviousZero() {
        when(authenticationLogRepository.countDistinctUsersBetween(any(), any(), any()))
                .thenReturn(5L, 0L);
        when(authenticationLogRepository.countDistinctUsersGroupedByDay(any(), any(), eq("UTC"), any()))
                .thenReturn(List.of());

        GetDailyLoginsMetricsResponseDto result = useCase.execute();

        assertEquals(5L, result.getValue());
        assertEquals(100d, result.getTrendPct());
        assertEquals(List.of(0d, 0d, 0d, 0d, 0d, 0d, 0d), result.getSeries7d());
    }
}
