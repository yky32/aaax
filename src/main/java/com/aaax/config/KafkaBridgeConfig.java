package com.aaax.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Kafka when OTP channel=kafka <em>or</em> identity events kafka enabled.
 */
@Configuration
@Conditional(KafkaBridgeConfig.KafkaEnabledCondition.class)
public class KafkaBridgeConfig {

    @Bean
    ProducerFactory<String, String> aaaxKafkaProducerFactory(
            @Value("${aaax.events.kafka.bootstrap-servers:${aaax.otp.kafka.bootstrap-servers:${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}}}")
            String bootstrap) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> aaaxKafkaProducerFactory) {
        return new KafkaTemplate<>(aaaxKafkaProducerFactory);
    }

    static class KafkaEnabledCondition extends AnyNestedCondition {
        KafkaEnabledCondition() {
            super(ConfigurationCondition.ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(name = "aaax.otp.channel", havingValue = "kafka")
        static class OtpKafka {
        }

        @ConditionalOnProperty(name = "aaax.events.kafka.enabled", havingValue = "true")
        static class EventsKafka {
        }
    }
}
