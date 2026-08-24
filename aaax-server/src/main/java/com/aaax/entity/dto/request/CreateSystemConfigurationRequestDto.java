package com.aaax.entity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateSystemConfigurationRequestDto {
    private String id;
    private String name;
    private String target;
    private String scope;
    private Object value;
}
