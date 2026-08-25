package com.aaax.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaUtil {

    @Autowired
    private KafkaTemplate<String, String> kafkaProducer;

    public void send(String topicName, Object data) {
        try {
            String _data = JSONUtil.writeValue(data);
            kafkaProducer.send(topicName, _data);
            log.info("-- KafkaUtil.kafkaProducer [{}] => [{}]", topicName, _data);
        } catch (Exception exception) {
            exception.printStackTrace();
            log.error("-- Error in KafkaUtil.kafkaProducer exception [{}]", exception.getMessage());
            log.error("-- Error in KafkaUtil.kafkaProducer Error [{}] => [{}]", topicName, data);
        }
    }
}
