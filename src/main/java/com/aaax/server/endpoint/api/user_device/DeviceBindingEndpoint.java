package com.aaax.server.endpoint.api.user_device;

import com.aaax.core.entity.dto.aaax.response.GetUserDeviceResponseDto;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.utils.JwtUtil;
import com.aaax.server.entity.dto.request.RegisterUserDeviceRequestDto;
import com.aaax.server.usecase.UserDeviceUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class DeviceBindingEndpoint {

    private final UserDeviceUseCase userDeviceUseCase;

    @PostMapping("/user-devices/device-bindings")
    public Result<GetUserDeviceResponseDto> post(
            @Valid @RequestBody RegisterUserDeviceRequestDto requestDto
    ) {
        String userId = JwtUtil.userId();
        return R.success(userDeviceUseCase.doDeviceBinding(userId, requestDto));
    }
}
