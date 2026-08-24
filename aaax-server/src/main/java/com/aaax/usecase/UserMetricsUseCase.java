package com.aaax.usecase;

import com.aaax.core.entity.dto.uaa.response.*;
import com.aaax.core.utils.IdSplitter;
import com.aaax.service.UaaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserMetricsUseCase {

    private final UaaService uaaService;
    private final UserPreferenceUseCase userPreferenceUseCase;
    private final UserDeviceUseCase userDeviceUseCase;
    private final UserProfileUseCase userProfileUseCase;
    @Autowired
    @Lazy
    private UserIdentityVerificationUseCase userIdentityVerificationUseCase;

    public GetUserMetricsResponseDto execute(String userId, String sourceSystem) {
        GetUserResponseDto user = uaaService.get(Long.valueOf(IdSplitter.split(userId)));
        GetUserPreferenceResponseDto userPreference = userPreferenceUseCase.getUserPreference(userId, "general");
        GetUserDeviceResponseDto userDevice = Optional.ofNullable(sourceSystem).isPresent() ? userDeviceUseCase.myDevicesOfSourceSystem(userId, sourceSystem) : null;
        GetUserProfileResponseDto userProfile = userProfileUseCase.getUserProfile(userId);
        List<GetUserVerificationResponseDto> userVerifications = userIdentityVerificationUseCase.myVerifications(userId);
        return GetUserMetricsResponseDto.builder()
                .user(user)
                .preference(userPreference)
                .device(userDevice)
                .profile(userProfile)
                .verifications(userVerifications)
                .build();
    }
}
