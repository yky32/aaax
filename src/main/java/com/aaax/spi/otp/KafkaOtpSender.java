package com.aaax.spi.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Kafka channel: delivery is the Identity Event Bus (Kafka sink). No second publish.
 */
@Component
@ConditionalOnProperty(name = "aaax.otp.channel", havingValue = "kafka")
public class KafkaOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(KafkaOtpSender.class);

    @Override
    public void send(String destination, String code) {
        log.debug("OTP channel=kafka — skip direct send (event bus already published) dest={}", destination);
    }
}
