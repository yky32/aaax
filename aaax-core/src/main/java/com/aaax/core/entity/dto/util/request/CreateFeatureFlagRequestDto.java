package com.aaax.core.entity.dto.util.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateFeatureFlagRequestDto {
    private String owner;  // can be # system, tenant, feature
    private String description;
    private String key;
    private Object context;
}