package com.aaax.core.entity.dto.util.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetFeatureFlagResponseDto {
    private String owner;
    private String key;
    private Object context;
    private Boolean isEnabled;
}