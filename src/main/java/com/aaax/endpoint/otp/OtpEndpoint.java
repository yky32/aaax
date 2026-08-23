package com.aaax.endpoint.otp;

import com.aaax.entity.dto.request.RequestOtpRequestDto;
import com.aaax.entity.dto.request.VerifyOtpRequestDto;
import com.aaax.entity.dto.response.RequestOtpResponseDto;
import com.aaax.entity.dto.response.VerifyOtpResponseDto;
import com.aaax.usecase.otp.RequestOtpUseCase;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/otp")
public class OtpEndpoint {

    private final RequestOtpUseCase requestOtpUseCase;

    public OtpEndpoint(RequestOtpUseCase requestOtpUseCase) {
        this.requestOtpUseCase = requestOtpUseCase;
    }

    @PostMapping("/request")
    public RequestOtpResponseDto request(@Valid @RequestBody RequestOtpRequestDto body) {
        return requestOtpUseCase.execute(body.username());
    }

    @PostMapping("/verify")
    public VerifyOtpResponseDto verify(@Valid @RequestBody VerifyOtpRequestDto body) {
        return requestOtpUseCase.verify(body.username(), body.code());
    }
}
