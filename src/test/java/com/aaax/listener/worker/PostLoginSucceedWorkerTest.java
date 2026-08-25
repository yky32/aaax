package com.aaax.listener.worker;

import com.aaax.core.kafka.event.PostLoginSucceedEvent;
import com.aaax.core.utils.JSONUtil;
import com.aaax.repository.AuthenticationLogRepository;
import com.aaax.repository.UserTokenRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostLoginSucceedWorkerTest {

    @Mock private UserTokenRepository userTokenRepository;
    @Mock private AuthenticationLogRepository authenticationLogRepository;
    @Mock private Acknowledgment acknowledgment;

    @InjectMocks
    private PostLoginSucceedWorker worker;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(worker, "serviceName", "uaa");
    }

    @Test
    @DisplayName("execute should persist auth log and access/refresh tokens")
    void execute_shouldPersistTokens() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-01T00:00:01Z");
        long issued = start.getEpochSecond();
        long expires = start.plusSeconds(3600).getEpochSecond();

        Map<String, Object> responseBody = Map.of(
                "user", Map.of(
                        "username", "user@test.com",
                        "grantType", Map.of("value", "custom-password-grant")
                ),
                "token", Map.of(
                        "registeredClient", Map.of("id", "rc-1"),
                        "accessToken", Map.of(
                                "tokenValue", "access-token",
                                "issuedAt", (double) issued,
                                "expiresAt", (double) expires
                        ),
                        "refreshToken", Map.of(
                                "tokenValue", "refresh-token",
                                "issuedAt", (double) issued,
                                "expiresAt", (double) expires
                        )
                )
        );

        PostLoginSucceedEvent event = PostLoginSucceedEvent.builder()
                .userId("u_10")
                .requestId("req-1")
                .domain("user-authentication")
                .event("custom-password-grant")
                .startTrafficDt(start)
                .endTrafficDt(end)
                .requestBody(Map.of("username", "user@test.com"))
                .responseBody(responseBody)
                .build();

        when(authenticationLogRepository.saveAndFlush(any())).thenAnswer(inv -> {
            var log = inv.getArgument(0);
            ReflectionTestUtils.setField(log, "correlationId", "10");
            return log;
        });

        worker.execute(new ConsumerRecord<>("t", 0, 0L, "k", JSONUtil.writeValue(event)), acknowledgment);

        verify(authenticationLogRepository).saveAndFlush(any());
        ArgumentCaptor<List> tokensCaptor = ArgumentCaptor.forClass(List.class);
        verify(userTokenRepository).saveAll(tokensCaptor.capture());
        assertEquals(2, tokensCaptor.getValue().size());
    }

    @Test
    @DisplayName("execute should skip token save when token map empty")
    void execute_shouldSkipWhenNoTokens() {
        PostLoginSucceedEvent event = PostLoginSucceedEvent.builder()
                .userId("u_10")
                .requestId("req-2")
                .domain("user-authentication")
                .event("custom-password-grant")
                .startTrafficDt(Instant.now().minusSeconds(1))
                .endTrafficDt(Instant.now())
                .responseBody(Map.of("user", Map.of("username", "u"), "token", Map.of()))
                .build();
        when(authenticationLogRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        worker.execute(new ConsumerRecord<>("t", 0, 0L, "k", JSONUtil.writeValue(event)), acknowledgment);

        verify(userTokenRepository, never()).saveAll(any());
    }
}
