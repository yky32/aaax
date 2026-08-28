package com.aaax.server.entity.dto.json_context;
import com.aaax.core.common.jsonfield.PermissionMetadata;
import com.aaax.core.constant.enu.aaax.Authorities;
import com.aaax.core.constant.enu.aaax.PermissionEffect;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class PermissionDbMetadata {
    private PermissionEffect effect;
    private List<Authorities> authorities;

    public PermissionDbMetadata(PermissionMetadata permissionMetadata) {
        this.effect = permissionMetadata.getEffect();
        this.authorities = permissionMetadata.getAuthorities();
    }
}
