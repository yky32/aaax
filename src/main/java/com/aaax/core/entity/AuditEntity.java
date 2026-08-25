package com.aaax.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;


@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(value = AuditingEntityListener.class)
public class AuditEntity implements Serializable {
    @Version
    @Column(name = "version")
    protected int version;

    @Column(name = "create_dt")
    @CreatedDate
    protected Instant createDt;

    @Column(name = "created_by")
    @CreatedBy
    protected String createdBy;
    // dynamically use string to store the user key, String.valueOf(user long id)
    // example, "6821123112312578", "1000010201", "UUID"

    @Column(name = "update_dt")
    @LastModifiedDate
    protected Instant updateDt;

    @Column(name = "updated_by")
    @LastModifiedBy
    protected String updatedBy;
}