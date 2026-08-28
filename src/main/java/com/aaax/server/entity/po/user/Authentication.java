package com.aaax.server.entity.po.user;

import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.entity.AuditEntityWithIsActive;
import com.aaax.server.entity.dto.json_context.CredentialHistoryMetadata;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.aaax.core.utils.generator.id.SnowflakeId;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @SnowflakeId
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
    private Instant lastLoginDt;

    @Column
    private Integer attempts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<CredentialHistoryMetadata> credentialsHistories;
}