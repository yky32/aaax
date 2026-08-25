package com.aaax.server.endpoint.api.tenant_use;

import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.server.entity.dto.request.CreateUserRouteMgtRequestDto;
import com.aaax.server.entity.dto.response.GetUserRouteResponseDto;
import com.aaax.server.usecase.GetUserUseCase;
import com.aaax.server.usecase.UserRouteUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserForTenantEndpoint {

    // ===== hotfix
    private final GetUserUseCase getUserUseCase;
    private final UserRouteUseCase userRouteUseCase;

    @GetMapping("/users/tenant-role-routes")
    public Result<List<GetUserResponseDto>> searchByTrrIds(
            @RequestParam List<String> trrIds
    ) {
        return R.success(getUserUseCase.searchByTrrIds(trrIds));
    }

    @GetMapping("/users/{id}/tenant-role-routes-ids")
    public Result<List<Long>> getTrrIdsByUserId(
            @PathVariable String id
    ) {
        return R.success(getUserUseCase.getTrrIds(id));
    }
    // ===== hotfix

    /**
     *
     * @param userId - the user
     * @param requestDto - storing the trrId
     * @return - GetUserRouteResponseDto.class
     */
    @PostMapping("/users/{userId}/routes")
    public Result<GetUserRouteResponseDto> create(
            @PathVariable String userId,
            @Valid @RequestBody CreateUserRouteMgtRequestDto requestDto
    ) {
        return R.success(userRouteUseCase.createUserRoute(userId, requestDto));
    }
}
