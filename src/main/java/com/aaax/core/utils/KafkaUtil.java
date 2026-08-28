package com.aaax.core.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaUtil {

    private final ObjectProvider<KafkaTemplate<String, String>> kafkaProducer;

    public void send(String topicName, Object data) {
        KafkaTemplate<String, String> template = kafkaProducer.getIfAvailable();
        if (template == null) {
            log.debug("Kafka disabled — skip send to {}", topicName);
            return;
        }
        try {
            String _data = JSONUtil.writeValue(data);
            template.send(topicName, _data);
            log.info("-- KafkaUtil.kafkaProducer [{}] => [{}]", topicName, _data);
        } catch (Exception exception) {
            log.error("-- Error in KafkaUtil.kafkaProducer exception [{}]", exception.getMessage());
            log.error("-- Error in KafkaUtil.kafkaProducer Error [{}] => [{}]", topicName, data);
        }
    }
}
