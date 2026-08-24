package com.aaax.core.entity.dto.util.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PutFeatureFlagRequestDto {
    private String description;
    private Object context;
}