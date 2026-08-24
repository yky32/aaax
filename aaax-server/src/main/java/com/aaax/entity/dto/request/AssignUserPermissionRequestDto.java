package com.aaax.entity.dto.request;

import com.aaax.core.common.jsonfield.PermissionMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AssignUserPermissionRequestDto {
    private String userId;
    private List<PermissionMetadata> permissions;
}
