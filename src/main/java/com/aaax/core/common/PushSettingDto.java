package com.aaax.core.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PushSettingDto {
    private String userId;
    private String phone;
    private String fcmToken;
    private String locale;
    private String theme;
    private String timezone; // default UTC
    private Map<String, Map> platformPreferences = new HashMap<>();
}
