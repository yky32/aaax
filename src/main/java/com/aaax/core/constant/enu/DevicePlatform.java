package com.aaax.core.constant.enu;


import lombok.Getter;

@Getter
public enum DevicePlatform {
    ANDROID("Android"),
    IOS("iOS"),
    WINDOWS("Windows"),
    MACOS("macOS"),
    LINUX("Linux"),
    CHROME_OS("Chrome OS"),
    FIRE_OS("Fire OS"),
    TIZEN("Tizen"),
    RASPBERRY_PI("Raspberry Pi"),
    OTHER("Other"),
    UNKNOWN("UNKNOWN")
    ;

    private final String displayName;

    DevicePlatform(String displayName) {
        this.displayName = displayName;
    }
}
