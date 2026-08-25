package com.aaax.endpoint.api.user_preference;

import com.aaax.core.entity.dto.uaa.response.GetUserPreferenceResponseDto;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.utils.JwtUtil;
import com.aaax.entity.dto.request.UpdateUserPreferenceRequestDto;
import com.aaax.usecase.UserPreferenceUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceEndpoint {

    private final UserPreferenceUseCase userPreferenceUseCase;

    @GetMapping({
            "/user-preferences/my-preferences",
            "/user-preferences/users/{userId}/my-preferences"
    })
    public Result<GetUserPreferenceResponseDto> get(
            @RequestParam(required = false) String key,
            @PathVariable(value = "userId", required = false) String userId
    ) {
        return getUserPreference(key, userId);
    }

    /** Resolves {@code key} (default {@code general}) and {@code userId}: path wins when present, else JWT subject. */
    private Result<GetUserPreferenceResponseDto> getUserPreference(String key, String userId) {
        key = Optional.ofNullable(key).orElse("general");
        userId = StringUtils.hasText(userId) ? userId : JwtUtil.userId();
        return R.success(userPreferenceUseCase.getUserPreference(userId, key));
    }

    @PutMapping("/user-preferences/my-preferences")
    public Result<GetUserPreferenceResponseDto> update(
            @RequestParam(required = false) String key,
            @RequestBody UpdateUserPreferenceRequestDto putDto
    ) {
        key = Optional.ofNullable(key).orElse("general");
        String userId = JwtUtil.userId();
        return R.success(userPreferenceUseCase.updateUserPreference(userId, key, putDto));
    }

    @PutMapping("/user-preferences/my-preferences/{preference}")
    public Result<GetUserPreferenceResponseDto> update(
            @RequestParam(required = false) String key,
            @PathVariable String preference,
            @RequestBody Map<String, Object> putDto
    ) {
        key = Optional.ofNullable(key).orElse("general");
        String userId = JwtUtil.userId();
        return R.success(userPreferenceUseCase.updateUserPreference(userId, key, preference, putDto));
    }
}
