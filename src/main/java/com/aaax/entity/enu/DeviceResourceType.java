package com.aaax.entity.enu;


import lombok.Getter;

@Getter
public enum DeviceResourceType {
    SYSTEM("System"),
    PRODUCT("Product")
    ;

    private final String displayName;

    DeviceResourceType(String displayName) {
        this.displayName = displayName;
    }

}
