package com.aaax.core.entity.dto.aaax.response;

import com.aaax.core.entity.dto.BaseResponseDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuperBuilder
public class GetUserMetricsResponseDto extends BaseResponseDto {
    private GetUserResponseDto user;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private GetUserPreferenceResponseDto preference;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private GetUserDeviceResponseDto device;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private GetUserProfileResponseDto profile;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<GetUserVerificationResponseDto> verifications;
}
