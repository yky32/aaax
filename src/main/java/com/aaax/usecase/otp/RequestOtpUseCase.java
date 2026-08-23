package com.aaax.usecase.otp;

import com.aaax.entity.dto.response.RequestOtpResponseDto;
import com.aaax.entity.dto.response.VerifyOtpResponseDto;

import org.springframework.stereotype.Component;

/** OTP request entry (delegates domain ops). */
@Component
public class RequestOtpUseCase {

    private final OtpOpsUseCase otpOpsUseCase;

    public RequestOtpUseCase(OtpOpsUseCase otpOpsUseCase) {
        this.otpOpsUseCase = otpOpsUseCase;
    }

    public RequestOtpResponseDto execute(String username) {
        return otpOpsUseCase.request(username);
    }

    public VerifyOtpResponseDto verify(String username, String code) {
        return otpOpsUseCase.verify(username, code);
    }
}
