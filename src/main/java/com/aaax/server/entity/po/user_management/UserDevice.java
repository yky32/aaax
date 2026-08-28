package com.aaax.server.entity.po.user_management;

import com.aaax.core.common.jsonfield.DeviceMetadata;
import com.aaax.core.entity.AuditEntityWithIsActive;
import com.aaax.core.utils.generator.id.SnowflakeId;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
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
@Table(name = "user_devices", uniqueConstraints = {
        @UniqueConstraint(name = "unique_key", columnNames = {"resourceId", "resourceType", "userId"})
})
@Builder
public class UserDevice extends AuditEntityWithIsActive {

    @Id
    @Column
    @SnowflakeId
    private Long id;

    @Column
    private String resourceId;
    @Column
    private String resourceType;
    @Column
    private Long userId;

    // ________________
    // then operate it later on this field
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<DeviceMetadata> context;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map control;
}