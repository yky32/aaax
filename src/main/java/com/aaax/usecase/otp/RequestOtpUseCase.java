package com.aaax.usecase.otp;

import com.aaax.entity.dto.response.OtpRequestResponse;
import com.aaax.service.OtpService;
import com.aaax.entity.dto.response.OtpVerifyResponse;

import org.springframework.stereotype.Component;

/** Thin application entry for OTP request/verify (delegates to OtpService domain helper). */
@Component
public class RequestOtpUseCase {

    private final OtpService otpService;

    public RequestOtpUseCase(OtpService otpService) {
        this.otpService = otpService;
    }

    public OtpRequestResponse execute(String username) {
        return otpService.request(username);
    }

    public OtpVerifyResponse verify(String username, String code) {
        return otpService.verify(username, code);
    }
}
