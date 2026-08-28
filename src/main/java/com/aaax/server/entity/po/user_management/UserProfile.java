package com.aaax.server.entity.po.user_management;

import com.aaax.core.entity.AuditEntityWithIsActive;
import com.aaax.server.entity.enu.UserProfileType;
import com.aaax.core.utils.generator.id.SnowflakeId;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "user_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uniqueProfileKey", columnNames = {"userId", "type"})
})
@Builder
public class UserProfile extends AuditEntityWithIsActive {

    @Id
    @Column
    @SnowflakeId
    private Long id;

    @Column
    private Long userId;

    @Column
    private String type = UserProfileType.DEFAULT.name();

    @Column(unique = true)
    private String alias; // = nickname
    // ________________
    // then operate it later on this field
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Object context;
}