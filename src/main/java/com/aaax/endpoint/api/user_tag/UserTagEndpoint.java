package com.aaax.endpoint.api.user_tag;

import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.service.CommonService;
import com.aaax.usecase.UserTagsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserTagEndpoint {

    private final UserTagsUseCase userTagsUseCase;
    private final CommonService commonService;

    @PatchMapping("/users/{id}/source-system-tags")
    @PreAuthorize("permitAll()")
    public Result ssTags(
            @RequestParam String ss,
            @PathVariable String id
    ) {
        commonService.isValidSourceSystem(ss);
        return R.success(userTagsUseCase.assign(id, ss));
    }
}
