package com.aaax.server.entity.dto.request;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateRbacTemplateRequestDto {
    private String name;
    private String description;
    private Map<String, Object> permissions;
}
