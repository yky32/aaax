package com.aaax.endpoint.api;

import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.entity.dto.request.QrCodeLoginRequestDto;
import com.aaax.usecase.QrCodeLoginUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@Slf4j
public class DeviceEndpoint {
    @Autowired
    private QrCodeLoginUseCase qrCodeLoginUseCase;

    @PostMapping("/qr-code-login")
    @PreAuthorize("permitAll()")
    public Result qrCodeLogin(@RequestBody QrCodeLoginRequestDto requestDto) {
        qrCodeLoginUseCase.qrCodeLogin(requestDto);
        return R.success();
    }
}
