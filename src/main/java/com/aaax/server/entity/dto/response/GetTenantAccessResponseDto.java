package com.aaax.server.entity.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class is the API requestBody Mapping object.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetTenantAccessResponseDto {
    private String id;
    private String tenantId;
    private String targetType; // #USER_ID
    private String targetId; // #user_id
    private Object context;
}
