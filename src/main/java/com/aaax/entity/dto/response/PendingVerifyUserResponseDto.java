package com.aaax.entity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.aaax.core.entity.dto.BaseResponseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuperBuilder
public class PendingVerifyUserResponseDto extends BaseResponseDto {
    private String username;
    private String code;
    private Integer ttl;
}
