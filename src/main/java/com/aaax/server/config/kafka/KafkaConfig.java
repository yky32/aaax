package com.aaax.server.config.kafka;

import com.aaax.core.config.kafka.KafkaConsumerConfig;
import com.aaax.core.config.kafka.KafkaProducerConfig;
import com.aaax.core.utils.KafkaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;

import java.io.InputStream;
import java.security.KeyStore;

@Configuration
@Slf4j
public class KafkaConfig {

    @Bean
    public KafkaUtil kafkaUtil() {
        return new KafkaUtil();
    }

    @Bean
    public KafkaProducerConfig kafkaProducerConfig() {
        return new KafkaProducerConfig();
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        return kafkaProducerConfig().kafkaAdmin();
    }

    @Bean
    public KafkaConsumerConfig kafkaConsumerConfig() {
        return new KafkaConsumerConfig();
    }

    @Bean
    public KafkaListenerContainerFactory consumerFactory(ConsumerFactory<String, String> consumerFactory) {
        return kafkaConsumerConfig().factory(consumerFactory);
    }
}