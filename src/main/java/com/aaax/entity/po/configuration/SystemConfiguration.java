package com.aaax.entity.po.configuration;

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
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uniqueTargetAndScope", columnNames = {"target", "scope"})
})
@Builder
public class SystemConfiguration extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "api_key_id_generator", strategy = "com.aaax.core.utils.generator.id.SnowflakeIdGenerator")
    @GeneratedValue(generator = "api_key_id_generator")
    private Long id;
    @Column
    private String name; // user-register.otp
    @Column
    private String target; // otp
    @Column
    private String scope; // otp.global
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object value; // user-register.otp
}