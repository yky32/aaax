package com.aaax.server.entity.po;

import com.aaax.core.entity.AuditEntityWithIsActive;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

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
@Table(name = "user_routes", uniqueConstraints = {@UniqueConstraint(name = "unique_userId_tenantRoleRouteId", columnNames = {"userId", "tenantRoleRouteId"})})
@Builder
public class UserRoute extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "user_route_id_generator", strategy = "com.aaax.core.utils.generator.id.SnowflakeIdGenerator")
    @GeneratedValue(generator = "user_route_id_generator")
    private Long id;
    @Column
    private Long userId;
    @Column
    private Long tenantRoleRouteId; // from [TenantRoleWithRoute.class] id in [TENANT-SERVICE]

    // ________________
    // will copy from [RouteTemplate.class] at new CREATE,
    // then operate it later on this field
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object actualRoutes;
}