package com.aaax.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class LoggingIdentityEventSink implements IdentityEventSink {

    private static final Logger log = LoggerFactory.getLogger(LoggingIdentityEventSink.class);

    @Override
    public void publish(IdentityEvent event) {
        log.info("AAAX event type={} subject={} id={}", event.type(), event.subject(), event.id());
    }
}
