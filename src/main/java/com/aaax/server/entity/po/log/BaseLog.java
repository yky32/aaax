package com.aaax.server.entity.po.log;

import com.aaax.core.aop.log.LogScope;
import com.aaax.core.constant.enu.LogType;
import com.aaax.core.entity.AuditEntity;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

/**
 * TYPE: activity / audit trail
 * SCOPE: int / ext
 * REQUEST_CONTEXT: ip, user-agent, device, api, request-id, headers, path, requstBody, start_dt, end_dt
 * SYSTEM: uaa, payment-service, tenant-service, util
 * DOMAIN: user
 * EVENT: user.created
 * CONTENT: logging content
 * NEW_CONTENT: delta change
 * DELTA: _________________ differences
 * ACTION_BY: triggered
 * TRAFFIC_TIME: 1s.
 * METADATA: others info.
 * TRACE_ID: the searchable identifier
 */
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
@Data
public class BaseLog extends AuditEntity {

    @Column
    @Enumerated(EnumType.STRING)
    private LogType type;
    @Column
    @Enumerated(EnumType.STRING)
    private LogScope logScope;
    @Column
    private String scope;
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object requestContext;
    @Column
    private String system;
    @Column
    private String domain;
    @Column
    private String event;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object requestBody; // log content
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object responseBody; // log content

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object content; // log content
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object newContent; // log content
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object delta;

    @Column
    private String actionBy; // = userId
    @Column
    private Long trafficTimeInMilliseconds;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object metadata;
    @Column
    private String traceId;
    @Column
    private String correlationId;

    // for sharding
    @Column
    private String shardingName; // e.g. TenantKey
    @Column
    private Long shardingKey; // 78231721737271273
}