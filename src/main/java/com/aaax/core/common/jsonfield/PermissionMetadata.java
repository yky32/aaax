package com.aaax.core.common.jsonfield;


import com.aaax.core.constant.enu.OperationAction;
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
public class PermissionMetadata {
    private String key;
    private PermissionEffect effect = PermissionEffect.ALLOW;
    private List<Authorities> authorities;
    private Boolean isOverride = false;
    private OperationAction dbOperation;
}
