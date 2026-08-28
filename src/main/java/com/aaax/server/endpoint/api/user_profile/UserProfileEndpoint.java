package com.aaax.server.endpoint.api.user_profile;

import com.aaax.core.entity.dto.aaax.response.GetUserProfileResponseDto;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.utils.JwtUtil;
import com.aaax.server.entity.dto.request.UpdateUserProfileRequestDto;
import com.aaax.server.usecase.UserProfileUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/user-profiles")
@RequiredArgsConstructor
@Slf4j
public class UserProfileEndpoint {

    private final UserProfileUseCase userProfileUseCase;
    @Value("${aaax.config.system-invoker}")
    protected String systemInvoker;

    /**
     *
     * @param a - aspect
     * @return
     */
    @GetMapping("/my-profiles")
    public Result<GetUserProfileResponseDto> getMyProfiles(
            @RequestParam(required = false) List<String> a
    ) {
        a = Optional.ofNullable(a).isEmpty() ? List.of() : a;
        String userId = JwtUtil.userId();
        return getUserProfiles(userId, a);
    }

    @GetMapping("/users/{userId}/my-profiles")
    public Result<GetUserProfileResponseDto> getUserProfiles(
            @PathVariable String userId,
            @RequestParam(required = false) List<String> a
    ) {
        a = Optional.ofNullable(a).isEmpty() ? List.of() : a;
        return R.success(userProfileUseCase.getUserProfile(userId, a));
    }


    /**
     * Get profile by alias
     * @param alias
     * @return
     */
    @GetMapping("/{alias}")
    public Result<GetUserProfileResponseDto> getProfile(
            @PathVariable String alias
    ) {
        return R.success(userProfileUseCase.getOneProfile(alias));
    }

    /**
     * Update my profile with icon
     * @param requestDto
     * @param ss - system source
     * @return
     */
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<GetUserProfileResponseDto> updateMyProfileWithIcon(@ModelAttribute UpdateUserProfileRequestDto requestDto,
                                                                     @RequestParam(required = false) String ss) {
        String userId = JwtUtil.userId();
        ss = Optional.ofNullable(ss).isEmpty() ? systemInvoker : ss;
        return R.success(userProfileUseCase.updateUserProfile(userId, requestDto, (String) ((Map<?, ?>) JwtUtil.getFromJwt(JwtUtil.METADATA)).get("identifier"), ss));
    }

    /**
     * Update my profile
     * @param requestDto
     * @param ss - system source
     * @return
     */
    @PutMapping
    public Result<GetUserProfileResponseDto> updateMyProfile(@RequestBody UpdateUserProfileRequestDto requestDto,
                                                             @RequestParam(required = false) String ss) {
        String userId = JwtUtil.userId();
        ss = Optional.ofNullable(ss).isEmpty() ? systemInvoker : ss;
        return R.success(userProfileUseCase.updateUserProfile(userId, requestDto, (String) ((Map<?, ?>) JwtUtil.getFromJwt(JwtUtil.METADATA)).get("identifier"), ss));
    }
}
