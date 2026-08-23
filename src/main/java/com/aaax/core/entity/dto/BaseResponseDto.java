package com.aaax.core.entity.dto;

import java.time.Instant;

import com.aaax.core.entity.AuditEntity;
import com.aaax.core.entity.AuditEntityWithIsActive;

/**
 * Shared audit fields on API responses (ledger/qs style).
 * Reuse via composition in {@code *ResponseDto} records — no feature-local audit copy.
 */
public record BaseResponseDto(
        Instant createDt,
        Instant updateDt,
        String createdBy,
        String updatedBy,
        Boolean isActive
) {
    public static BaseResponseDto from(AuditEntity entity) {
        if (entity == null) {
            return null;
        }
        Boolean active = null;
        if (entity instanceof AuditEntityWithIsActive a) {
            active = a.getIsActive();
        }
        return new BaseResponseDto(
                entity.getCreateDt(),
                entity.getUpdateDt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                active);
    }
}
