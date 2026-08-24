package com.aaax.entity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class GetTenantRoleWithRouteResponseDto {
    private String id;
    private String tenantId;
    private String tenantKey;
    private String role;
    private String routeTemplateId;
}