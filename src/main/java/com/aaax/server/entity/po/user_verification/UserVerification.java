package com.aaax.server.entity.po.user_verification;

import com.aaax.core.constant.enu.UserVerificationStatus;
import com.aaax.core.entity.AuditEntityWithIsActive;
import com.aaax.core.utils.generator.id.SnowflakeId;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_verifications", uniqueConstraints = {
        @UniqueConstraint(name = "unique_key", columnNames = {"extIdentifier", "userId"})
})
@Builder
public class UserVerification extends AuditEntityWithIsActive {

    @Id
    @Column
    @SnowflakeId
    private Long id;
    @Column
    private Long userId;
    @Column
    private String extIdentifier;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Object detail; // user-register.otp
    @Enumerated(EnumType.STRING)
    @Column
    private UserVerificationStatus status;
}