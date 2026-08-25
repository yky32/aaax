package com.aaax.core.entity.dto.uaa.response;

import com.aaax.core.common.jsonfield.DeviceMetadata;
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
public class GetUserDeviceResponseDto extends BaseResponseDto {
    private String id;
    private String userId;
    private List<DeviceMetadata> context;
}
