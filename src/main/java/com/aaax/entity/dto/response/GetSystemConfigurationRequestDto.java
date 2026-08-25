package com.aaax.entity.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetSystemConfigurationRequestDto {
    private String id;
    private String name;
    private String target;
    private String scope;
    private Object value;
    private Boolean isActive;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "en_US", timezone = "UTC")
    private Instant createDt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "en_US", timezone = "UTC")
    private Instant updateDt;
}
