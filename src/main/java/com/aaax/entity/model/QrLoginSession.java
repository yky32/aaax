package com.aaax.entity.model;

import java.time.Instant;

/**
 * Non-persistent domain model (not JPA).
 * In-flight QR login session — desktop pending phone approval.
 * Lives under {@code entity/model}, not {@code entity/po}.
 */
public final class QrLoginSession {

    public enum Status {
        PENDING,
        APPROVED,
        CONSUMED,
        EXPIRED
    }

    private final String id;
    private final String userCode;
    private final Instant expiresAt;
    private Status status = Status.PENDING;
    private String approvedUsername;

    public QrLoginSession(String id, String userCode, Instant expiresAt) {
        this.id = id;
        this.userCode = userCode;
        this.expiresAt = expiresAt;
    }

    public String id() {
        return id;
    }

    public String userCode() {
        return userCode;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Status status() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String approvedUsername() {
        return approvedUsername;
    }

    public void setApprovedUsername(String approvedUsername) {
        this.approvedUsername = approvedUsername;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
