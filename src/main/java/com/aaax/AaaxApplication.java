package com.aaax;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Kafka producer is only constructed when {@code aaax.otp.channel=kafka}
 * ({@link com.aaax.config.KafkaOtpConfig}). Default console/mail/sms need no broker.
 */
@SpringBootApplication
public class AaaxApplication {

    public static void main(String[] args) {
        SpringApplication.run(AaaxApplication.class, args);
    }
}
