package com.aaax.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Mode 1 — no SMS adapter inside AAAX: publish OTP to Kafka for caller's notification-service.
 */
@Component
@ConditionalOnProperty(name = "aaax.otp.channel", havingValue = "kafka")
public class KafkaOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(KafkaOtpSender.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final String issuer;
    private final tools.jackson.databind.ObjectMapper objectMapper;

    public KafkaOtpSender(
            KafkaTemplate<String, String> kafkaTemplate,
            tools.jackson.databind.ObjectMapper objectMapper,
            @Value("${aaax.otp.kafka.topic:aaax.otp.dispatch}") String topic,
            @Value("${aaax.issuer:http://localhost:8081}") String issuer) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.issuer = issuer;
    }

    @Override
    public void send(String destination, String code) {
        try {
            OtpDispatchEvent event = new OtpDispatchEvent(
                    OtpDispatchEvent.TYPE,
                    destination,
                    destination,
                    "kafka",
                    code,
                    "otp",
                    java.time.Instant.now().plusSeconds(300),
                    issuer);
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, destination, json);
            log.info("AAAX OTP published topic={} key={}", topic, destination);
        } catch (Exception ex) {
            log.error("Kafka OTP publish failed — console fallback dest={} code={}", destination, code, ex);
            log.info("AAAX OTP for {} => {}", destination, code);
        }
    }
}
