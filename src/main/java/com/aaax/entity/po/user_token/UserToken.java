package com.aaax.entity.po.user_token;

import com.aaax.core.entity.AuditEntityWithIsActive;
import com.aaax.entity.enu.UserTokenType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

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
    @GenericGenerator(name = "user_token_id_generator", strategy = "com.aaax.core.utils.generator.id.SnowflakeIdGenerator")
    @GeneratedValue(generator = "user_token_id_generator")
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
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private Instant issuedAt;
    @Column
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private Instant expireAt;
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object value;
}