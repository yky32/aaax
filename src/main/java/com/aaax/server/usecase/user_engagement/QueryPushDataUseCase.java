package com.aaax.server.usecase.user_engagement;

import com.aaax.core.common.PushSettingDto;
import com.aaax.core.entity.dto.uaa.response.GetUserDeviceResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserPreferenceResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserProfileResponseDto;
import com.aaax.core.utils.JSONUtil;
import com.aaax.server.usecase.ResultUseCase;
import com.aaax.server.usecase.UserDeviceUseCase;
import com.aaax.server.usecase.UserPreferenceUseCase;
import com.aaax.server.usecase.UserProfileUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueryPushDataUseCase implements ResultUseCase<Long, PushSettingDto, Void> {

    @Value("${aaax.config.system-invoker}")
    private String systemInvoker;
    private final UserDeviceUseCase userDeviceUseCase;
    private final UserProfileUseCase userProfileUseCase;
    private final UserPreferenceUseCase userPreferenceUseCase;

    @Override
    public PushSettingDto execute(Long userId) {
        String userIdStr = String.valueOf(userId);
        List<GetUserDeviceResponseDto> userDevices = userDeviceUseCase.myDevices(userIdStr, systemInvoker);
        GetUserProfileResponseDto userProfile = userProfileUseCase.getUserProfile(userIdStr);
        GetUserPreferenceResponseDto userPreference = userPreferenceUseCase.getUserPreference(userIdStr, "general");
        Map userProfileContext = JSONUtil.convertFromObject(userProfile.getContext(), Map.class);
        Map userPreferenceContext = JSONUtil.convertFromObject(userPreference.getContext(), Map.class);

        // New users may have no device rows yet (onboarding before FCM registration).
        String fcmToken = userDevices.stream()
                .findFirst()
                .flatMap(device -> Optional.ofNullable(device.getContext()).orElse(List.of()).stream().findFirst())
                .map(context -> context.getToken().getOrDefault("fcm", "NA-NOT-FOUND"))
                .orElse("NA-NOT-FOUND");
        String phone = String.valueOf(userProfileContext.getOrDefault("phone", "NA-NOT-FOUND"));
        String locale = (String) ((Map) userPreferenceContext.getOrDefault("localizations", new HashMap<>())).getOrDefault("selected", "en");
        String theme = (String) ((Map) userPreferenceContext.getOrDefault("themes", new HashMap<>())).getOrDefault("selected", "SYSTEM");
        Map<String, Map> platformPreferences = ((Map) userPreferenceContext.getOrDefault("notifications", new HashMap<>()));
        return PushSettingDto.builder()
                .phone(phone)
                .fcmToken(fcmToken)
                .userId("u_".concat(userIdStr))
                .locale(locale)
                .theme(theme)
                .platformPreferences(platformPreferences)
                .build();
    }
}
