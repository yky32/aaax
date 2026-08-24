package com.aaax.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetRbacTemplateResponseDto {
    private String id;
    private String name;
    private String description;
    private Object permissions;
}
