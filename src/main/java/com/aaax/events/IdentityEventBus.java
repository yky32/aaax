package com.aaax.events;

import java.util.List;
import java.util.Map;

import com.aaax.audit.AuditService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Single entry — audit DB + fan-out sinks (log / kafka / webhook).
 * This is the product wedge: identity signals for platform notification-service.
 */
@Service
public class IdentityEventBus {

    private final AuditService auditService;
    private final List<IdentityEventSink> sinks;
    private final String issuer;

    public IdentityEventBus(
            AuditService auditService,
            List<IdentityEventSink> sinks,
            @Value("${aaax.issuer:http://localhost:8081}") String issuer) {
        this.auditService = auditService;
        this.sinks = sinks;
        this.issuer = issuer;
    }

    public IdentityEvent emit(String type, String subject, String detail, Map<String, Object> data) {
        IdentityEvent event = IdentityEvent.of(issuer, type, subject, data);
        auditService.record(type, subject, detail);
        for (IdentityEventSink sink : sinks) {
            try {
                sink.publish(event);
            } catch (Exception ignored) {
                // sinks log their own failures
            }
        }
        return event;
    }

    public IdentityEvent emit(String type, String subject, Map<String, Object> data) {
        return emit(type, subject, null, data);
    }
}
