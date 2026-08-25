package com.aaax.core.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuperBuilder
public class BaseResponseDto {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "en_US", timezone = "UTC")
    protected Instant createDt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "en_US", timezone = "UTC")
    protected Instant updateDt;
    protected String createBy;
    protected String updateBy;
}
