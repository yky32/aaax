package com.aaax.core.entity.dto.uaa.response;

import com.aaax.core.constant.enu.UserVerificationStatus;
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
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetUserVerificationResponseDto extends BaseResponseDto {
    private String id;
    private String userId;
    private Object detail;
    private UserVerificationStatus status;
}
