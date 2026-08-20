package com.aaax.otp.application;

import com.aaax.otp.OtpRequestResponse;
import com.aaax.otp.OtpService;
import com.aaax.otp.OtpVerifyResponse;

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
