package com.aaax.server.entity.po.user_token;

import com.aaax.core.entity.AuditEntityWithIsActive;
import com.aaax.server.entity.enu.UserTokenType;
import com.aaax.core.utils.generator.id.SnowflakeId;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_tokens")
@Builder
public class UserToken extends AuditEntityWithIsActive {

    @Id
    @Column
    @SnowflakeId
    private Long id;
    @Column
    private String traceId; // the exact traceId of the event
    @Column
    private String correlationId; // connection to the log id.
    @Column
    private Long userId;
    @Enumerated(EnumType.STRING)
    @Column
    private UserTokenType type;
    @Column
    private Instant issuedAt;
    @Column
    private Instant expireAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Object value;
}