package com.aaax.server.endpoint.api.my_roles;

import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.server.entity.dto.response.GetMyRolesResponseDto;
import com.aaax.server.usecase.GetMyRolesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class MyRolesEndpoint {

    private final GetMyRolesUseCase getMyRolesUseCase;

    @GetMapping("/users/my-roles")
    public Result<GetMyRolesResponseDto> getMyRoles() {
        String userId = com.aaax.core.utils.JwtUtil.userId();
        return R.success(getMyRolesUseCase.execute(userId));
    }
}
