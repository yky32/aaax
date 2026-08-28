package com.aaax.server.endpoint.api.mgt;

import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.entity.dto.aaax.response.GetUserResponseDto;
import com.aaax.core.response.PaginationDto;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.server.entity.dto.request.RegisterUserRequestDto;
import com.aaax.server.entity.dto.request.UpdatePasswordRequestDto;
import com.aaax.server.entity.dto.request.UpdateUsernameRequestDto;
import com.aaax.server.entity.dto.request.UpdateUserStatusRequestDto;
import com.aaax.server.entity.dto.response.GetAuthenticationLogResponseDto;
import com.aaax.server.usecase.QueryUserAuthenticationLogsUseCase;
import com.aaax.server.usecase.RegisterUserUseCase;
import com.aaax.server.usecase.UserManagementUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/mgt")
public class MgtUserEndpoint {

    private final UserManagementUseCase userManagementUseCase;
    private final RegisterUserUseCase registerUserUseCase;
    private final QueryUserAuthenticationLogsUseCase queryUserAuthenticationLogsUseCase;

    @PostMapping("/users")
    public Result<GetUserResponseDto> register(@Valid @RequestBody RegisterUserRequestDto requestDto) {
        return R.success(registerUserUseCase.execute(requestDto));
    }

    @PatchMapping("/users/{identifier}/credentials")
    @PreAuthorize("isAuthenticated()")
    public Result<GetUserResponseDto> updateCredentials(
            @Valid @RequestBody UpdatePasswordRequestDto requestDto,
            @PathVariable String identifier
    ) {
        return R.success(userManagementUseCase.updateCredentials(requestDto, identifier));
    }

    /**
     * Admin rename login identifier (username / email / mobile).
     * Path {@code identifier} is the current login id; body {@code username} is the new value.
     */
    @PatchMapping("/users/{identifier}/username")
    @PreAuthorize("isAuthenticated()")
    public Result<GetUserResponseDto> updateUsername(
            @Valid @RequestBody UpdateUsernameRequestDto requestDto,
            @PathVariable String identifier
    ) {
        return R.success(userManagementUseCase.updateUsername(requestDto, identifier));
    }

    @PatchMapping("/users/{identifier}/statuses")
    @PreAuthorize("isAuthenticated()")
    public Result<GetUserResponseDto> updateStatuses(
            @Valid @RequestBody UpdateUserStatusRequestDto requestDto,
            @PathVariable String identifier
    ) {
        UserStatus.get(requestDto.getStatus());
        return R.success(userManagementUseCase.updateStatuses(requestDto, identifier));
    }

    @GetMapping("/users")
    @PreAuthorize("isAuthenticated()")
    public Result<List<GetUserResponseDto>> getAllUsers(
            @PageableDefault(page = 1, size = 100, sort = "createDt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String startDt,
            @RequestParam(required = false) String endDt,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String query
    ) {
        PaginationDto.PaginationDtoBuilder result = userManagementUseCase.getAllUsers(pageable, startDt, endDt, tenantId, query);
        return R.success((List<GetUserResponseDto>) result.build().getData(), result.build().getPagination());
    }

    /**
     * Login / auth activity trail for a single user ({@code authentication_log}).
     */
    @GetMapping("/users/{userId}/authentication-logs")
    @PreAuthorize("isAuthenticated()")
    public Result<List<GetAuthenticationLogResponseDto>> getAuthenticationLogs(
            @PathVariable String userId,
            @PageableDefault(page = 1, size = 20, sort = "createDt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String startDt,
            @RequestParam(required = false) String endDt,
            @RequestParam(required = false) String event
    ) {
        PaginationDto.PaginationDtoBuilder result = queryUserAuthenticationLogsUseCase.execute(
                userId, pageable, startDt, endDt, event);
        return R.success(
                (List<GetAuthenticationLogResponseDto>) result.build().getData(),
                result.build().getPagination()
        );
    }

    @DeleteMapping("/users/identifier/{identifier}")
    @PreAuthorize("isAuthenticated()")
    public Result<String> deleteUserByUsername(
            @PathVariable String identifier,
            @RequestParam(required = false) Boolean isSoftDelete
    ) {
        isSoftDelete = Optional.ofNullable(isSoftDelete).isPresent() ? isSoftDelete : false;
        userManagementUseCase.deleteByIdentifier(identifier, isSoftDelete);
        return R.success(identifier.concat(" was deleted."));
    }

    @DeleteMapping("/users/id/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<String> deleteUserByUserId(
            @PathVariable String id,
            @RequestParam(required = false) Boolean isSoftDelete
    ) {
        isSoftDelete = Optional.ofNullable(isSoftDelete).isPresent() ? isSoftDelete : false;
        userManagementUseCase.deleteByUserId(id, isSoftDelete);
        return R.success(id.concat(" was deleted. isSoftDelete => %s".formatted(isSoftDelete)));
    }

    @DeleteMapping("/users/internal-testing")
    @PreAuthorize("isAuthenticated()")
    public Result<String> deleteUserByInternalTestingUserId() {
        int numberOfUsers = userManagementUseCase.testingDeleteAll();
        String count = "[%s] was/were being deleted.".formatted(numberOfUsers);
        return R.success(count);
    }


}
