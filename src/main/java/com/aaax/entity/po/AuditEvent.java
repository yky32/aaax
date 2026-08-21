package com.aaax.entity.po;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import com.aaax.events.IdentityEvent;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @Column(length = 36)
    private String id;

    /** Correlates to IdentityEvent.id when emitted via the bus. */
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(nullable = false, length = 128)
    private String action;

    @Column(length = 64)
    private String actor;

    @Column(length = 512)
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    public AuditEvent(String eventId, String action, String actor, String detail) {
        this.eventId = eventId;
        this.action = action;
        this.actor = actor;
        this.detail = detail;
    }

    /** Legacy convenience — no event correlation. */
    public AuditEvent(String action, String actor, String detail) {
        this(null, action, actor, detail);
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAction() {
        return action;
    }

    public String getActor() {
        return actor;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
