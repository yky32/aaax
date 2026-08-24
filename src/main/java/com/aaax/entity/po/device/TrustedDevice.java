package com.aaax.entity.po.device;

import java.time.Instant;

import com.aaax.core.entity.AuditEntityWithIsActive;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(indexes = {
        @Index(name = "idx_device_account", columnList = "accountId"),
        @Index(name = "idx_device_token", columnList = "tokenHash", unique = true)
})
public class TrustedDevice extends AuditEntityWithIsActive {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String accountId;

    /** SHA-256 hex of the opaque device cookie value. */
    @Column(nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(length = 128)
    private String label;

    @Column(length = 512)
    private String userAgent;

    @Column(length = 64)
    private String ip;

    @Column(nullable = false)
    private Instant lastSeenAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column
    private Instant revokedAt;

    @PrePersist
    void preDevice() {
        if (lastSeenAt == null) {
            lastSeenAt = Instant.now();
        }
    }

    /** Cookie still valid (not revoked / not expired). Soft {@code isActive} is separate. */
    public boolean isValid() {
        return Boolean.TRUE.equals(getIsActive())
                && revokedAt == null
                && Instant.now().isBefore(expiresAt);
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

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
