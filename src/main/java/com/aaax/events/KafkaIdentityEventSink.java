package com.aaax.events;

import tools.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(10)
@ConditionalOnBean(KafkaTemplate.class)
public class KafkaIdentityEventSink implements IdentityEventSink {

    private static final Logger log = LoggerFactory.getLogger(KafkaIdentityEventSink.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaIdentityEventSink(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${aaax.events.kafka.topic:${aaax.otp.kafka.topic:aaax.identity.events}}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(IdentityEvent event) {
        try {
            String key = event.subject() != null ? event.subject() : event.id();
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(event));
            log.debug("Identity event kafka type={} topic={}", event.type(), topic);
        } catch (Exception ex) {
            log.warn("Identity event kafka failed type={}: {}", event.type(), ex.toString());
        }
    }
}
