package com.aaax.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntityWithIsActive extends AuditEntity {
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @PrePersist
    private void isActive() {
        this.isActive = true;
    }
}