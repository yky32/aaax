package com.aaax.entity.po.session;

import java.time.Instant;

import com.aaax.core.entity.AuditEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(indexes = {
        @Index(name = "idx_session_account", columnList = "accountId"),
        @Index(name = "idx_session_token", columnList = "sessionToken", unique = true)
})
public class AuthSession extends AuditEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String accountId;

    @Column(nullable = false, length = 64, unique = true)
    private String sessionToken;

    @Column(length = 512)
    private String userAgent;

    @Column(length = 64)
    private String ip;

    @Column(nullable = false)
    private Instant lastSeenAt;

    @Column
    private Instant revokedAt;

    @PrePersist
    void onCreate() {
        if (lastSeenAt == null) {
            lastSeenAt = Instant.now();
        }
    }

    public boolean isSessionActive() {
        return revokedAt == null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
