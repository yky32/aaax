package com.aaax.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Mode 1 SMS path: OTP → Kafka topic → caller's notification-service (own SMS provider).
 * Only active when {@code aaax.otp.channel=kafka}.
 */
@Configuration
@ConditionalOnProperty(name = "aaax.otp.channel", havingValue = "kafka")
public class KafkaOtpConfig {

    @Bean
    ProducerFactory<String, String> otpProducerFactory(
            @Value("${aaax.otp.kafka.bootstrap-servers:${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}}") String bootstrap) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    KafkaTemplate<String, String> otpKafkaTemplate(ProducerFactory<String, String> otpProducerFactory) {
        return new KafkaTemplate<>(otpProducerFactory);
    }
}
