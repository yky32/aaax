package com.aaax.core.entity.dto.uaa.response;

import com.aaax.core.entity.dto.BaseResponseDto;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class GetKeysResponseDto extends BaseResponseDto {
    private String publicKey;
    private String privateKey;
}
