package com.aaax.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

/**
 * Audit base + soft-active flag (qs/uaa {@code AuditEntityWithIsActive}).
 */
@MappedSuperclass
public abstract class AuditEntityWithIsActive extends AuditEntity {

    @Column(nullable = false)
    private Boolean isActive;

    @PrePersist
    void applyIsActiveDefault() {
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }
}
