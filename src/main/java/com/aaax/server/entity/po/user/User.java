package com.aaax.server.entity.po.user;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.aaax.core.common.jsonfield.UserMetadata;
import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.entity.AuditEntityWithIsActive;
import com.aaax.core.utils.generator.id.SnowflakeId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * We hope to provide multiple-username login feature
 * This PO is to generate user_id
 * Link this PO with [Authentication.clas] --> derived from user_id to more than one login username
 * [user] 1:n [authentication]
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
@Builder
public class User extends AuditEntityWithIsActive {

    @Id
    @Column
    @SnowflakeId
    private Long id;

    @Column(unique = true)
    private String username; //email, mobile

    @Enumerated(EnumType.STRING)
    @Column
    private UserStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private UserMetadata metadata;

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonIgnore
    private List<Authentication> authentications = new ArrayList<>();

    public void addAuthentication(Authentication authentication) {
        // __ duplication logic
        boolean isExisted = authentications.stream().anyMatch(auth -> auth.getLoginType().equals(authentication.getLoginType()));
        if (!isExisted) {
            authentications.add(authentication);
        }
    }

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> sourceSystemTags;
}