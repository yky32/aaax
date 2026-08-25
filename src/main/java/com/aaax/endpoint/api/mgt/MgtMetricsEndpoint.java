package com.aaax.endpoint.api.mgt;

import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.entity.dto.response.GetDailyLoginsMetricsResponseDto;
import com.aaax.usecase.metrics.QueryDailyLoginsMetricsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/mgt")
public class MgtMetricsEndpoint {

    private final QueryDailyLoginsMetricsUseCase queryDailyLoginsMetricsUseCase;

    @GetMapping("/metrics/daily-logins")
    @PreAuthorize("isAuthenticated()")
    public Result<GetDailyLoginsMetricsResponseDto> dailyLogins() {
        return R.success(queryDailyLoginsMetricsUseCase.execute());
    }
}
