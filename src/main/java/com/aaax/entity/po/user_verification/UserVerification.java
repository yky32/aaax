package com.aaax.entity.po.user_verification;

import com.aaax.core.constant.enu.UserVerificationStatus;
import com.aaax.core.entity.AuditEntityWithIsActive;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;


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
    @GenericGenerator(name = "user_verification_id_generator", strategy = "com.aaax.core.utils.generator.id.SnowflakeIdGenerator")
    @GeneratedValue(generator = "user_verification_id_generator")
    private Long id;
    @Column
    private Long userId;
    @Column
    private String extIdentifier;
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object detail; // user-register.otp
    @Enumerated(EnumType.STRING)
    @Column
    private UserVerificationStatus status;
}