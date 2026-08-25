package com.aaax.server.endpoint.api.authentication;

import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.utils.JwtUtil;
import com.aaax.server.entity.dto.request.AddLinkedAuthenticationRequestDto;
import com.aaax.server.entity.dto.request.UserAuthenticationCheckRequestDto;
import com.aaax.server.entity.dto.response.GetLinkedAuthenticationResponseDto;
import com.aaax.server.usecase.UserAuthenticationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Account security: list / link / unlink login methods for the current user (email ACO + social).
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class UserAuthenticationEndpoint {

    private final UserAuthenticationUseCase userAuthenticationUseCase;

    @PostMapping("/users/my-authentication-checks")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> myAuthentications(
            @RequestBody UserAuthenticationCheckRequestDto dto
    ) {
        return R.success(userAuthenticationUseCase.authenticate(dto));
    }

    /**
     * Link a login method to the current user.
     * <ul>
     *   <li>Social: {@code { "provider": "google"|"apple", "idToken": "..." }}</li>
     *   <li>Legacy: {@code { "username": "...", "credentials": "..." }}</li>
     * </ul>
     */
    @PostMapping("/users/my-linked-authentications")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> addMyLinkedAuthentications(
            @RequestBody AddLinkedAuthenticationRequestDto dto
    ) {
        String userId = JwtUtil.userId();
        userAuthenticationUseCase.addLinkedAuthentications(userId, dto);
        return R.success(true);
    }

    /**
     * Linked methods for settings UI (loginType, identifier, canUnlink).
     */
    @GetMapping("/users/my-linked-authentications")
    @PreAuthorize("isAuthenticated()")
    public Result<List<GetLinkedAuthenticationResponseDto>> fetchMyLinkedAuthentications() {
        String userId = JwtUtil.userId();
        return R.success(userAuthenticationUseCase.fetchLinkedAuthentications(userId));
    }

    /**
     * Unlink a method, e.g. {@code DELETE /users/my-linked-authentications/GOOGLE}.
     * EMAIL/USERNAME cannot be unlinked; cannot remove last method.
     */
    @DeleteMapping("/users/my-linked-authentications/{loginType}")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> unlinkMyLinkedAuthentication(
            @PathVariable String loginType
    ) {
        String userId = JwtUtil.userId();
        userAuthenticationUseCase.unlinkAuthentication(userId, loginType);
        return R.success(true);
    }
}
