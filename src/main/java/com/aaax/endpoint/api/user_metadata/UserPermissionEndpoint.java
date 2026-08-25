package com.aaax.endpoint.api.user_metadata;

import com.aaax.core.common.jsonfield.PermissionMetadata;
import com.aaax.core.constant.enu.OperationAction;
import com.aaax.core.constant.enu.uaa.Authorities;
import com.aaax.core.constant.enu.uaa.PermissionEffect;
import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.event.UserPermissionMutatedEvent;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.response.SystemResponse;
import com.aaax.core.utils.JwtUtil;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.entity.dto.request.AssignUserRoleRequestDto;
import com.aaax.entity.dto.response.GetUserPermissionResponseDto;
import com.aaax.usecase.AccessControlUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.aaax.core.kafka.enu.KafkaTopic.USER_USER_PERMISSION_MUTATED;

@RestController
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class UserPermissionEndpoint {

    private final AccessControlUseCase accessControlUseCase;
    private final KafkaUtil kafkaUtil;

    // ** Need to do re-assessment after adding roles within.
    @PostMapping("/users/permissions")
    public Result<GetUserPermissionResponseDto> assignUserPermissions(
            @Valid @RequestBody UserPermissionMutatedEvent requestDto
    ) {
        return R.success(accessControlUseCase.assignPermissionToUser(requestDto));
    }

    @PostMapping("/users/{userId}/roles")
    public Result<GetUserPermissionResponseDto> assignUserRoles(
            @PathVariable String userId,
            @Valid @RequestBody AssignUserRoleRequestDto dto
    ) {
        List<String> predefinedRoles = List.of("admin", "normal");// later will put it into DB.
        for (String role : dto.getRoles()) {
            boolean isContained = predefinedRoles.contains(role);
            if (!isContained) {
                String message = "Invalid predefinedRoles: ".concat(role);
                throw new BizException(SystemResponse.PAM0400, Map.of("message", message, "options", predefinedRoles));
            }
        }
        return R.success(accessControlUseCase.assignRoleToUser(userId, dto));
    }

    @GetMapping("/users/permissions/test")
    public Result<String> test(
            @RequestParam String key,
            @RequestParam Boolean isOverride,
            @RequestParam List<Authorities> authorities,
            @RequestParam PermissionEffect effect,
            @RequestParam OperationAction operationAction

    ) {
        // =====================
        UserPermissionMutatedEvent event = UserPermissionMutatedEvent.builder()
                .eventName(USER_USER_PERMISSION_MUTATED)
                .userId(JwtUtil.userId())
                .permissions(List.of(
                        PermissionMetadata.builder()
                                .key(key)
                                .authorities(Optional.ofNullable(authorities).orElse(List.of()))
                                .effect(effect)
                                .isOverride(isOverride)
                                .dbOperation(operationAction)
                                .build()))
                .build();
        kafkaUtil.send(USER_USER_PERMISSION_MUTATED, event);
        // =====================
        return R.success("OK");
    }
}
