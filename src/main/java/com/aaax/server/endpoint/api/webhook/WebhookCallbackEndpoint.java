package com.aaax.server.endpoint.api.webhook;


import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.server.usecase.UserIdentityVerificationUseCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/webhooks")
@AllArgsConstructor
@Slf4j
public class WebhookCallbackEndpoint {

    private final UserIdentityVerificationUseCase userIdentityVerificationUseCase;

    @PostMapping("/uaa/{userId}/activations")
    public Result<String> webhooks(
            @PathVariable String userId,
            @RequestBody Object data
    ) {
        log.info("-- webhook ==> {}, userId: {}", data, userId);
        userIdentityVerificationUseCase.complete(userId, data);
        return R.success();
    }
}
