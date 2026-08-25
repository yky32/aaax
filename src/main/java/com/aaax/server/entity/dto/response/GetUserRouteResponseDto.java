package com.aaax.server.entity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.aaax.core.common.i18n.i18n;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class GetUserRouteResponseDto {
    private String id;
    private String userId;
    private String tenantId;
    private i18n tenantName;
    private String tenantKey;
    private String role;
    private Object routes;
}
