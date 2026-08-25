package com.aaax.server.entity.po.user;

import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.entity.AuditEntityWithIsActive;
import com.aaax.server.entity.dto.json_context.CredentialHistoryMetadata;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "authentications",
    uniqueConstraints = {@UniqueConstraint(name = "unique_userId_loginType_identifier", columnNames = {"user_id", "loginType", "identifier"})}
)
@Builder
public class Authentication extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "user_authentication_id_generator", strategy = "com.aaax.core.utils.generator.id.SnowflakeIdGenerator")
    @GeneratedValue(generator = "user_authentication_id_generator")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private User user;

    @Column
    private String identifier; // username for each login_type

    @Enumerated(EnumType.STRING)
    @Column
    private LoginType loginType;

    @Column(columnDefinition = "text")
    private String credentials;

    // Metadata Dt.
    @Column
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private Instant lastLoginDt;

    @Column
    private Integer attempts;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<CredentialHistoryMetadata> credentialsHistories;
}