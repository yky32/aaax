package com.aaax.entity.po.user_management;

import com.aaax.core.entity.AuditEntityWithIsActive;
import com.aaax.entity.enu.UserProfileType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

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
    @GenericGenerator(name = "user_profile_id_generator", strategy = "com.aaax.core.utils.generator.id.SnowflakeIdGenerator")
    @GeneratedValue(generator = "user_profile_id_generator")
    private Long id;

    @Column
    private Long userId;

    @Column
    private String type = UserProfileType.DEFAULT.name();

    @Column(unique = true)
    private String alias; // = nickname
    // ________________
    // then operate it later on this field
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object context;
}