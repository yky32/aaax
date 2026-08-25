package com.aaax.core.entity.dto.util.response;

import com.aaax.core.entity.dto.BaseResponseDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetRefDataResponseDto extends BaseResponseDto {
    private String key;
    private Object value;
    private Boolean isActive;
}
