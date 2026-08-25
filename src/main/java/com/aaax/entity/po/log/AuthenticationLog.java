package com.aaax.entity.po.log;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

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
@Data
@Entity
@Table(indexes = {
        @Index(name = "log_index", columnList = "traceId, correlationId"),
        @Index(name = "log_trace_index", columnList = "traceId"),
        @Index(name = "log_event_index", columnList = "event"),
        @Index(name = "log_domain_index", columnList = "domain"),
        @Index(name = "idx_auth_log_event_create_dt", columnList = "event, create_dt")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationLog extends BaseLog {

    @Id
    @Column
    @GenericGenerator(name = "log_id_generator", strategy = "com.aaax.core.utils.generator.id.SnowflakeIdGenerator")
    @GeneratedValue(generator = "log_id_generator")
    private Long id;
}