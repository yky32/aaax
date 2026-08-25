package com.aaax.server.endpoint.api.engagement;

import com.aaax.core.common.PushSettingDto;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.utils.IdSplitter;
import com.aaax.core.utils.JwtUtil;
import com.aaax.server.usecase.user_engagement.QueryPushDataUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserEngagementEndpoint {

    private final QueryPushDataUseCase queryPushDataUseCase;

    @GetMapping("/user-engagements/my-push-data")
    public Result<PushSettingDto> userPushData() {
        String userId = JwtUtil.userId();
        log.info("-- Querying push settings for user {}", userId);
        return this.userPushData(userId);
    }

    @GetMapping("/user-engagements/{userId}/my-push-data")
    public Result<PushSettingDto> userPushData(
            @PathVariable String userId
    ) {
        log.info("-- Querying push settings for user {}", userId);
        return R.success(queryPushDataUseCase.execute(IdSplitter.splitToLong(userId)));
    }
}
