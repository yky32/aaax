package com.aaax.endpoint.api.user_profile;

import com.aaax.core.entity.dto.uaa.response.GetUserProfileResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.response.SystemResponse;
import com.aaax.entity.dto.request.UpdateUserProfileRequestDto;
import com.aaax.usecase.UserProfileUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/mgt/user-profiles")
@RequiredArgsConstructor
@Slf4j
public class UserProfileMgtEndpoint {

    private final UserProfileUseCase userProfileUseCase;
    @Value("${config.system-invoker}")
    protected String systemInvoker;

    @PutMapping("/u/{userId}")
    public Result<GetUserProfileResponseDto> updateMyProfile(
            @PathVariable String userId,
            @RequestBody UpdateUserProfileRequestDto requestDto,
            @RequestParam(required = false) String ss
    ) {
        ss = Optional.ofNullable(ss).isEmpty() ? systemInvoker : ss;
        Optional.ofNullable(requestDto.getContext())
                .orElseThrow(() -> new BizException(SystemResponse.PAM0400, "Plz provide [%s]".formatted("context")));
        return R.success(userProfileUseCase.updateUserProfileMgt(userId, requestDto, ss));
    }
}
