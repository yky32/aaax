package com.aaax.core.utils;

import com.aaax.core.constant.enu.DevicePlatform;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class UserAgentUtil {

    public static DevicePlatform detectOperatingSystem(String userAgent) {
        if (userAgent == null) {
            return DevicePlatform.UNKNOWN;
        }
        // Check for Windows
        if (userAgent.contains("Windows")) {
            return DevicePlatform.WINDOWS;
        }
        // Check for macOS
        else if (userAgent.contains("Mac OS X")) {
            return DevicePlatform.MACOS;
        }
        // Check for Linux
        else if (userAgent.contains("Linux")) {
            return DevicePlatform.LINUX;
        }
        // Check for Android
        else if (userAgent.contains("Android")) {
            return DevicePlatform.ANDROID;
        }
        // Check for iOS
        else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return DevicePlatform.IOS;
        }
        return DevicePlatform.UNKNOWN;
    }
}
