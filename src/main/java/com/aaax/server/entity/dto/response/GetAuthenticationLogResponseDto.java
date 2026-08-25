package com.aaax.server.entity.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.aaax.core.aop.log.LogScope;
import com.aaax.core.constant.enu.LogType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Admin-facing row for UAA authentication activity (login success / fail attempts).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetAuthenticationLogResponseDto {

    private String id;
    private LogType type;
    private LogScope logScope;
    private String system;
    private String domain;
    private String event;
    private String actionBy;
    private String traceId;
    private String correlationId;
    private Long trafficTimeInMilliseconds;
    private Object content;
    private Object requestBody;
    private Object responseBody;
    private Object metadata;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "en_US", timezone = "UTC")
    private Instant createDt;
}
