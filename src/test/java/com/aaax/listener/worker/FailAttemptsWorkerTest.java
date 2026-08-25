package com.aaax.listener.worker;

import com.aaax.core.kafka.event.LoginAttemptsMutatedEvent;
import com.aaax.core.utils.JSONUtil;
import com.aaax.entity.po.user.Authentication;
import com.aaax.entity.po.user.User;
import com.aaax.repository.AuthenticationLogRepository;
import com.aaax.repository.AuthenticationRepository;
import com.aaax.service.AuthenticationService;
import jakarta.persistence.EntityManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailAttemptsWorkerTest {

    @Mock private AuthenticationRepository authenticationRepository;
    @Mock private AuthenticationService authenticationService;
    @Mock private AuthenticationLogRepository authenticationLogRepository;
    @Mock private EntityManager entityManager;
    @Mock private Acknowledgment acknowledgment;

    @InjectMocks
    private FailAttemptsWorker failAttemptsWorker;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(failAttemptsWorker, "serviceName", "uaa");
        ReflectionTestUtils.setField(failAttemptsWorker, "entityManager", entityManager);
    }

    @Test
    @DisplayName("execute should increment attempts on failure")
    void execute_shouldIncrementOnFailure() {
        Authentication auth = Authentication.builder()
                .identifier("user@test.com")
                .attempts(2)
                .user(User.builder().id(1L).build())
                .build();
        when(authenticationService.findValidRecordsByDynamicIdentifier("user@test.com")).thenReturn(auth);

        LoginAttemptsMutatedEvent event = LoginAttemptsMutatedEvent.builder()
                .username("User@test.com")
                .userId("1")
                .isSuccess(false)
                .requestId("req-1")
                .build();
        String json = JSONUtil.writeValue(event);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("t", 0, 0L, "k", json);

        failAttemptsWorker.execute(record, acknowledgment);

        assertEquals(3, auth.getAttempts());
        assertNotNull(auth.getLastLoginDt());
        verify(authenticationRepository).save(auth);
        verify(authenticationLogRepository).saveAndFlush(any());
        verify(entityManager).clear();
    }

    @Test
    @DisplayName("execute should reset attempts on success")
    void execute_shouldResetOnSuccess() {
        Authentication auth = Authentication.builder()
                .identifier("user@test.com")
                .attempts(5)
                .user(User.builder().id(1L).build())
                .build();
        when(authenticationService.findValidRecordsByDynamicIdentifier("user@test.com")).thenReturn(auth);

        LoginAttemptsMutatedEvent event = LoginAttemptsMutatedEvent.builder()
                .username("user@test.com")
                .userId("1")
                .isSuccess(true)
                .requestId("req-2")
                .build();
        ConsumerRecord<String, String> record = new ConsumerRecord<>("t", 0, 0L, "k", JSONUtil.writeValue(event));

        failAttemptsWorker.execute(record, acknowledgment);

        assertEquals(0, auth.getAttempts());
    }
}
