package com.aaax.server.endpoint.api.user_device;

import com.aaax.core.entity.dto.aaax.response.GetUserDeviceResponseDto;
import com.aaax.core.response.PaginationDto;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.utils.JwtUtil;
import com.aaax.server.entity.dto.request.RegisterUserDeviceRequestDto;
import com.aaax.server.usecase.UserDeviceUseCase;
import jakarta.validation.Valid;
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
public class UserDeviceEndpoint {

    private final UserDeviceUseCase userDeviceUseCase;
    @Value("${aaax.config.system-invoker}")
    protected String systemInvoker;

    @GetMapping("/mgt/user-devices/{id}")
    public Result<GetUserDeviceResponseDto> getOne(
            @PathVariable String id
    ) {
        return R.success(userDeviceUseCase.getOne(id));
    }

    @GetMapping("/mgt/user-devices")
    public Result<List<GetUserDeviceResponseDto>> getAll(
            @PageableDefault(page = 1, size = 100, sort = "createDt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String startDt,
            @RequestParam(required = false) String endDt
    ) {
        PaginationDto.PaginationDtoBuilder result = userDeviceUseCase.getAll(pageable, startDt, endDt);
        return R.success((List<GetUserDeviceResponseDto>) result.build().getData(), result.build().getPagination());
    }

    @GetMapping("/user-devices/my-devices")
    public Result<List<GetUserDeviceResponseDto>> myDevices(
            @RequestParam(required = false) String ss
    ) {
        ss = Optional.ofNullable(ss).isEmpty() ? systemInvoker : ss;
        String userId = JwtUtil.userId();
        return R.success(userDeviceUseCase.myDevices(userId, ss));
    }

    @PostMapping("/user-devices/my-devices")
    public Result<GetUserDeviceResponseDto> register(
            @RequestParam(required = false) String ss,
            @Valid @RequestBody RegisterUserDeviceRequestDto requestDto
    ) {
        requestDto.setSourceSystem(Optional.ofNullable(ss).isEmpty() ? systemInvoker : ss);
        String userId = JwtUtil.userId();
        return R.success(userDeviceUseCase.register(userId, requestDto));
    }
}
