package com.aaax.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaUtilTest {

    @Test
    @DisplayName("send should no-op when Kafka template is absent")
    @SuppressWarnings("unchecked")
    void send_shouldNoOpWhenDisabled() {
        ObjectProvider<KafkaTemplate<String, String>> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        KafkaUtil kafkaUtil = new KafkaUtil(provider);
        assertDoesNotThrow(() -> kafkaUtil.send("topic", "payload"));
    }
}
