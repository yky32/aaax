package com.aaax.server.entity.po.user_management;

import com.aaax.core.entity.AuditEntityWithIsActive;
import com.aaax.core.utils.generator.id.SnowflakeId;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * We hope to provide multiple-routes login feature
 * This PO is to generate user_id
 * Link this PO with [User.clas] --> derived from user_id to more than one login routes
 * [user] 1:n [routes] ==> user_permissions
 */

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_permissions", uniqueConstraints = {
        @UniqueConstraint(name = "uniqueKey", columnNames = {"userId"})
})
@Builder
public class UserPermission extends AuditEntityWithIsActive {

    @Id
    @Column
    @SnowflakeId
    private Long id;

    @Column
    private Long userId;

    @Column
    private String apiVersion;
    // ________________
    // will copy from [RbacTemplate.class] at new CREATE,
    // then operate it later on this field
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> actualPermissions; // the keys = roles name

}