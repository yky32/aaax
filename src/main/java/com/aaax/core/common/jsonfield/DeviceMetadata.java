package com.aaax.core.common.jsonfield;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceMetadata {
    private String deviceKey; //TODO #hashed.
    private DeviceProfileMetadata profile;
    private Integer seq;

    // === ON-DEMAND
    private Map<String, String> token; // FCM, SoftToken etc..
}
