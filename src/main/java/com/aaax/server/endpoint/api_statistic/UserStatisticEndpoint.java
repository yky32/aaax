package com.aaax.server.endpoint.api_statistic;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.PaginationDto;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.response.SystemResponse;
import com.aaax.server.repository.projection.UserInfoProjection;
import com.aaax.server.usecase.UserStatisticUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static com.aaax.server.entity.enu.AaaxAspect.USERS;

@RestController
@RequestMapping("/user-statistics")
@RequiredArgsConstructor
@Slf4j
public class UserStatisticEndpoint {

    private final UserStatisticUseCase userStatisticUseCase;

    @GetMapping
    public Result<List<UserInfoProjection>> get(
            @PageableDefault(page = 1, size = 100, sort = "createDt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PaginationDto.PaginationDtoBuilder result = userStatisticUseCase.getUsers(pageable);
        return R.success((List<UserInfoProjection>) result.build().getData(), result.build().getPagination());
    }

    @GetMapping("/users-count")
    public Result<Long> usersCount(
            @RequestParam(required = false) List<String> ss,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) String startDt,
            @RequestParam(required = false) String endDt,
            @RequestParam String a
    ) {
        boolean isValid = USERS.stream().anyMatch(profile -> profile.equalsIgnoreCase(a));
        if (!isValid) {
            throw new BizException(SystemResponse.PAM0400, "Invalid [%s] from [%s]".formatted(a, USERS));
        }
        ss = Optional.ofNullable(ss).isPresent()? ss: List.of() ;
        statuses = Optional.ofNullable(statuses).isPresent()? statuses : List.of("NA");
        return R.success(userStatisticUseCase.usersCount(ss, statuses, startDt, endDt, a));
    }
}
