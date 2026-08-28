package com.aaax.server.endpoint.api.user_verification;

import com.aaax.core.constant.enu.UserVerificationStatus;
import com.aaax.core.entity.dto.aaax.response.GetUserVerificationResponseDto;
import com.aaax.core.response.PaginationDto;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.utils.JwtUtil;
import com.aaax.server.usecase.UserIdentityVerificationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserVerificationEndpoint {

    private final UserIdentityVerificationUseCase userIdentityVerificationUseCase;
    @Value("${aaax.config.system-invoker}")
    protected String systemInvoker;

    @PatchMapping("/mgt/user-verifications/{id}/statuses")
    public Result<GetUserVerificationResponseDto> patchStatuses(
            @PathVariable String id,
            @RequestParam String status,
            @RequestParam(required = false) String ss
    ) {
        return R.success(userIdentityVerificationUseCase.patchStatuses(id, UserVerificationStatus.get(status), Optional.ofNullable(ss).isEmpty() ? systemInvoker : ss));
    }

    @GetMapping("/user-verifications")
    public Result<List<GetUserVerificationResponseDto>> getAll(
            @PageableDefault(page = 1, size = 100, sort = "createDt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String startDt,
            @RequestParam(required = false) String endDt
    ) {
        PaginationDto.PaginationDtoBuilder result = userIdentityVerificationUseCase.getAll(pageable, startDt, endDt);
        return R.success((List<GetUserVerificationResponseDto>) result.build().getData(), result.build().getPagination());
    }

    @GetMapping("/user-verifications/{id}")
    public Result<GetUserVerificationResponseDto> getOne(
            @PathVariable String id
    ) {
        return R.success(userIdentityVerificationUseCase.getOne(id));
    }

    @GetMapping("/user-verifications/my-verifications")
    public Result<List<GetUserVerificationResponseDto>> myVerifications() {
        String userId = JwtUtil.userId();
        return R.success(userIdentityVerificationUseCase.myVerifications(userId));
    }

    @GetMapping("/user-verifications/users/{id}")
    public Result<List<GetUserVerificationResponseDto>> getByUserId(@PathVariable String id) {
        return R.success(userIdentityVerificationUseCase.myVerifications(id));
    }
}
