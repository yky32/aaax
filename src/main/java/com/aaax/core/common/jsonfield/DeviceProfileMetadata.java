package com.aaax.core.common.jsonfield;

import com.aaax.core.constant.enu.DevicePlatform;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceProfileMetadata {
    private String displayName;
    private DevicePlatform platform;
    private String id; // device-id, uuid
    private String sid;
    private String serialNumber;
}
