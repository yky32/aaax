package com.aaax.endpoint.api.user_profile;

import com.aaax.core.entity.dto.uaa.response.GetUserProfileResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.utils.IdSplitter;
import com.aaax.core.utils.JwtUtil;
import com.aaax.exception.response.UserProfileErrorResponse;
import com.aaax.usecase.UserProfileUseCase;
import com.aaax.usecase.user_customization.UpdateAvatarUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * This endpoint is used to handle all mobile side customizations for user profile
 * such as update avatar, update cover, update profile info, etc.
 */
@RestController
@RequestMapping("/user-customizations")
@RequiredArgsConstructor
@Slf4j
public class UserCustomizationEndpoint {

    @Value("${config.system-invoker}")
    protected String systemInvoker;
    private final UpdateAvatarUseCase updateAvatarUseCase;
    private final UserProfileUseCase userProfileUseCase;

    @PatchMapping("/avatar")
    public Result<GetUserProfileResponseDto> updateAvatar(
            @RequestParam(required = false) List<MultipartFile> files
    ) {
        String userId = JwtUtil.userId();
        updateAvatarUseCase.execute(IdSplitter.splitToLong(userId), files);
        return R.success(userProfileUseCase.getUserProfile(userId));
    }

    @PatchMapping("/avatar-urls")
    public Result<GetUserProfileResponseDto> updateAvatar(
            @RequestBody Map<String, Object> dto
    ) {
        String userId = JwtUtil.userId();
        if (!dto.containsKey("url")) {
            throw new BizException(UserProfileErrorResponse.UPR0003, dto);
        }
        String avatarUrl = String.valueOf(dto.get("url"));
        updateAvatarUseCase.executeUrlOnly(IdSplitter.splitToLong(userId), avatarUrl);
        return R.success(userProfileUseCase.getUserProfile(userId));
    }

}
