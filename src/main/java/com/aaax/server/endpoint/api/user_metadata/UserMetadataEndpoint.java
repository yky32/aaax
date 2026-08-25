package com.aaax.server.endpoint.api.user_metadata;

import com.aaax.core.entity.dto.uaa.response.GetUserMetricsResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.utils.JwtUtil;
import com.aaax.server.entity.dto.request.UpdateAccessMetadataRequestDto;
import com.aaax.server.entity.dto.request.UpdateExtReferenceRequestDto;
import com.aaax.server.service.UaaService;
import com.aaax.server.usecase.UpdateUserMetadataUseCase;
import com.aaax.server.usecase.UserMetricsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserMetadataEndpoint {

    private final UpdateUserMetadataUseCase updateUserMetadataUseCase;
    private final UaaService uaaService;
    private final UserMetricsUseCase metricsUseCase;

    @PutMapping("/users/{userId}/metadata/ext-reference")
    @PreAuthorize("isAuthenticated()")
    public Result<GetUserResponseDto> updateExtReference(
            @Valid @RequestBody UpdateExtReferenceRequestDto requestDto,
            @PathVariable Long userId
    ) {
        return R.success(updateUserMetadataUseCase.updateExtReference(requestDto, userId));
    }

    @PutMapping("/users/{userId}/metadata/access")
    @PreAuthorize("isAuthenticated()")
    public Result<GetUserResponseDto> updateExtReference(
            @Valid @RequestBody UpdateAccessMetadataRequestDto requestDto,
            @PathVariable String userId
    ) {
        return R.success(updateUserMetadataUseCase.updateAccess(requestDto, userId));
    }

    @GetMapping("/users/me")
    @PreAuthorize("isAuthenticated()")
    public Result<GetUserResponseDto> getOne() {
        String userId = JwtUtil.userId();
        return R.success(uaaService.me(userId));
    }


    @GetMapping("/users/{userId}/my-metrics")
    @PreAuthorize("isAuthenticated()")
    public Result<GetUserMetricsResponseDto> getUserMetrics(
            @PathVariable String userId,
            @RequestParam(required = false) String ss
    ) {
        ss = Optional.ofNullable(ss).isEmpty() ? "GLOBAL" : ss;
        return R.success(metricsUseCase.execute(userId, ss));
    }

    @GetMapping("/users/my-metrics")
    @PreAuthorize("isAuthenticated()")
    public Result<GetUserMetricsResponseDto> getMyMetrics(
            @RequestParam(required = false) String ss
    ) {
        ss = Optional.ofNullable(ss).isEmpty() ? "GLOBAL" : ss;
        String userId = JwtUtil.userId();
        return R.success(metricsUseCase.execute(userId, ss));
    }
}
