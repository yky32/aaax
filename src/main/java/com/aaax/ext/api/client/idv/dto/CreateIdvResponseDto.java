package com.aaax.ext.api.client.idv.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateIdvResponseDto {
    private String id;
    private String status;
    private String accountId;
    private String workflowExecutionId;
}
