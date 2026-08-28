package com.aaax.server.entity.po.configuration;

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
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uniqueTargetAndScope", columnNames = {"target", "scope"})
})
@Builder
public class SystemConfiguration extends AuditEntityWithIsActive {

    @Id
    @Column
    @SnowflakeId
    private Long id;
    @Column
    private String name; // user-register.otp
    @Column
    private String target; // otp
    @Column
    private String scope; // otp.global
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Object value; // user-register.otp
}