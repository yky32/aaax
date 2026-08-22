package com.aaax.core.entity;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * qs/uaa + ledger practice: optimistic lock + Spring Data JPA auditing.
 * No {@code name} on {@code @Column} — trust Hibernate physical naming.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditEntity implements Serializable {

    @Version
    @Column
    protected int version;

    @Column
    @CreatedDate
    protected Instant createDt;

    @Column
    @CreatedBy
    protected String createdBy;

    @Column
    @LastModifiedDate
    protected Instant updateDt;

    @Column
    @LastModifiedBy
    protected String updatedBy;

    public int getVersion() {
        return version;
    }

    public Instant getCreateDt() {
        return createDt;
    }

    public void setCreateDt(Instant createDt) {
        this.createDt = createDt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getUpdateDt() {
        return updateDt;
    }

    public void setUpdateDt(Instant updateDt) {
        this.updateDt = updateDt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
