package com.aaax.otp;

import java.util.Map;

import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mode 1 — OTP as identity event (Kafka sink picks up). Caller owns SMS.
 */
@Component
@ConditionalOnProperty(name = "aaax.otp.channel", havingValue = "kafka")
public class KafkaOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(KafkaOtpSender.class);

    private final IdentityEventBus events;

    public KafkaOtpSender(IdentityEventBus events) {
        this.events = events;
    }

    @Override
    public void send(String destination, String code) {
        events.emit(
                IdentityEvent.Types.OTP_DISPATCH,
                destination,
                "otp via kafka event bus",
                Map.of(
                        "destination", destination,
                        "channel", "kafka",
                        "code", code,
                        "purpose", "otp"));
        log.info("AAAX OTP dispatched via identity event bus dest={}", destination);
    }
}
