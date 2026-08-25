package com.aaax.entity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserIdentityVerificationResultRequestDto {
    private String accountId;
    private String workflowExecutionId;
    private String tenantKey;
    private String sourceSystem;
}
