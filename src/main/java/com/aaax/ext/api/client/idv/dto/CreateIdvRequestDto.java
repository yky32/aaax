package com.aaax.ext.api.client.idv.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateIdvRequestDto {
    private String accountId;  // can be # system, tenant, feature
    private String workflowExecutionId;
    private String callbackUrl;
    private Map<String, Object> metadata;
}