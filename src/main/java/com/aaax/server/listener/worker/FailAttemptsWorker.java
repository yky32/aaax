package com.aaax.server.listener.worker;


import com.aaax.core.aop.log.LogScope;
import com.aaax.core.constant.enu.LogType;
import com.aaax.core.kafka.BaseListener;
import com.aaax.core.kafka.event.LoginAttemptsMutatedEvent;
import com.aaax.core.utils.JSONUtil;
import com.aaax.server.entity.po.log.AuthenticationLog;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.repository.AuthenticationLogRepository;
import com.aaax.server.repository.AuthenticationRepository;
import com.aaax.server.service.AuthenticationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

import static com.aaax.core.kafka.enu.KafkaTopic.USER_LOGIN_ATTEMPTS_MUTATED;


@Component
@Slf4j
@RequiredArgsConstructor
public class FailAttemptsWorker extends BaseListener {

    @Value("${ext.api.client.discord.webhookId}")
    private String webhookId;
    @Value("${ext.api.client.discord.webhookToken}")
    private String webhookToken;
    @Value("${spring.application.name}")
    private String serviceName;
    @PersistenceContext
    private EntityManager entityManager;
    private final AuthenticationRepository authenticationRepository;
    private final AuthenticationService authenticationService;
    private final AuthenticationLogRepository authenticationLogRepository;


    // ______ this is the execute method you need to better implemented
    @Override
    public void execute(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        LoginAttemptsMutatedEvent event = JSONUtil.readValue(payload.value(), LoginAttemptsMutatedEvent.class);
        entityManager.clear();
        Authentication authentication = authenticationService.findValidRecordsByDynamicIdentifier(event.getUsername().toLowerCase());
        int attempts;
        if (!event.getIsSuccess()) {
            attempts = Optional.ofNullable(authentication.getAttempts()).orElse(0) + 1;
        } else {
            attempts = 0;
        }
        log.info("========{}=============== {} @@ {}", attempts, this.getClass().getSimpleName(), event);
        authentication.setAttempts(attempts); // reset
        authentication.setLastLoginDt(Instant.now());
        authenticationRepository.save(authentication);

        AuthenticationLog _log = AuthenticationLog.builder().build();
        _log.setType(LogType.ACTIVITY);
        _log.setLogScope(LogScope.ENDPOINT);
        _log.setScope("INT");
        _log.setSystem(serviceName);
        _log.setDomain("user-authentication");
        _log.setEvent("login-attempts");
        _log.setRequestBody(event.getRequestBody());
        _log.setResponseBody(event.getResponseBody());
        _log.setContent(event);
        _log.setActionBy(event.getUserId());
        _log.setTraceId(event.getRequestId());
        _log.setCorrelationId(event.getUserId());
        authenticationLogRepository.saveAndFlush(_log);

    }

    @KafkaListener(
            topics = USER_LOGIN_ATTEMPTS_MUTATED, groupId = USER_LOGIN_ATTEMPTS_MUTATED + "-group",
            containerFactory = "consumerFactory"
    )
    private void action(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        // ___ *******
        // ____ plz keep this method --> in-order-to align all method for kafka behavior
        super.listener(payload, ack);
    }

    @Bean
    public NewTopic USER_LOGIN_ATTEMPTS_MUTATED() {
        return new NewTopic(USER_LOGIN_ATTEMPTS_MUTATED, 1, (short) 1);
    }
}
