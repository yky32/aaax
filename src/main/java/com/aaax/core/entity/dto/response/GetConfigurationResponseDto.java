package com.aaax.core.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetConfigurationResponseDto {
    private String id;
    private String target;
    private String scope;
    private Object value;
}