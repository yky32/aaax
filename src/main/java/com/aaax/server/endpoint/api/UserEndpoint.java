package com.aaax.server.endpoint.api;

import com.aaax.core.constant.RegexPatternConstant;
import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.response.PaginationDto;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.utils.IdSplitter;
import com.aaax.core.utils.ValidationUtil;
import com.aaax.server.entity.dto.request.UpdatePasswordRequestDto;
import com.aaax.server.service.UaaService;
import com.aaax.server.usecase.RegisterUserUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserEndpoint {

    private final UaaService uaaService;
    private final RegisterUserUseCase registerUserUseCase;
    @Value("${config.system-invoker}")
    protected String systemInvoker;

    /**
     * @param requestDto - the credentials
     * @param identifier - the login username
     * @return -
     */
    @PutMapping("/users/{identifier}/credentials")
    @PreAuthorize("isAuthenticated()")
    public Result<GetUserResponseDto> updateCredentials(
            @Valid @RequestBody UpdatePasswordRequestDto requestDto,
            @PathVariable String identifier
    ) {
        return R.success(registerUserUseCase.updateCredentials(requestDto, identifier));
    }

    @GetMapping("/users")
    @PreAuthorize("isAuthenticated()")
    public Result<List<GetUserResponseDto>> getAll(
            @PageableDefault(page = 1, size = Integer.MAX_VALUE, sort = "createDt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String startDt,
            @RequestParam(required = false) String endDt,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) List<String> ss,
            @RequestParam(required = false) List<String> a,
            @RequestParam(required = false) List<String> ids,
            @RequestParam(required = false) String namesQuery,
            @RequestParam(required = false) String emailQuery
    ) {
        PaginationDto.PaginationDtoBuilder result = uaaService.getAll(pageable, startDt, endDt, tenantId, ss, a, ids, query, namesQuery, emailQuery);
        return R.success((List<GetUserResponseDto>) result.build().getData(), result.build().getPagination());
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<GetUserResponseDto> getOne(
            @PathVariable String id,
            @RequestParam(required = false) String identifierType,
            @RequestParam(required = false) List<String> a,
            @RequestParam(required = false) String ss
    ) {
        a = Optional.ofNullable(a).isEmpty() ? new ArrayList<>() : a;
        ss = Optional.ofNullable(ss).isEmpty() ? systemInvoker : ss;

        if (StringUtils.isNotBlank(identifierType)) {
            return R.success(uaaService.getByIdentifierType(id, identifierType));
        }
        // Catering no jpa auditor case.
        if (
                id.equalsIgnoreCase("userId_not_existed") ||
                        !ValidationUtil.patternMatches(IdSplitter.split(id), RegexPatternConstant.IS_DIGIT)
        ) {
            GetUserResponseDto responseDto = GetUserResponseDto.builder()
                    .id("NA")
                    .username("NA")
                    .build();
            return R.success(responseDto);
        }
        return R.success(uaaService.getOne(id, a, ss));
    }
}
