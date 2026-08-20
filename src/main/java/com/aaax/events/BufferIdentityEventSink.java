package com.aaax.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * In-process ring buffer for admin portal + GET /v1/admin/events (no Kafka required).
 */
@Component
@Order(5)
public class BufferIdentityEventSink implements IdentityEventSink {

    private final int capacity;
    private final List<IdentityEvent> buffer;

    public BufferIdentityEventSink(@Value("${aaax.events.buffer-size:100}") int capacity) {
        this.capacity = Math.max(10, Math.min(capacity, 1000));
        this.buffer = new ArrayList<>();
    }

    @Override
    public synchronized void publish(IdentityEvent event) {
        buffer.add(0, event);
        while (buffer.size() > capacity) {
            buffer.remove(buffer.size() - 1);
        }
    }

    public synchronized List<IdentityEvent> recent(int limit) {
        int n = Math.min(Math.max(limit, 1), buffer.size());
        return Collections.unmodifiableList(new ArrayList<>(buffer.subList(0, n)));
    }

    public synchronized int size() {
        return buffer.size();
    }
}
