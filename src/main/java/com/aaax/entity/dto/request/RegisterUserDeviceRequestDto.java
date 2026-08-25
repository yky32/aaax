package com.aaax.entity.dto.request;


import com.aaax.core.common.jsonfield.DeviceMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterUserDeviceRequestDto {
    private DeviceMetadata device;
    private String sourceSystem;
}


