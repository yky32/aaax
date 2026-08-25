package com.aaax.entity.po.rbac;

import com.aaax.core.entity.AuditEntityWithIsActive;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import java.util.Map;

/**
 * We hope to provide multiple-routes login feature
 * This PO is to generate user_id
 * Link this PO with [User.clas] --> derived from user_id to more than one login routes
 * [user] 1:n [routes] ==> user_routes
 */

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
@Builder
public class RbacTemplate extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "route_template_id_generator", strategy = "com.aaax.core.utils.generator.id.SnowflakeIdGenerator")
    @GeneratedValue(generator = "route_template_id_generator")
    private Long id;

    @Column(unique = true)
    private String name;

    @Column
    private String description;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> permissions;
}