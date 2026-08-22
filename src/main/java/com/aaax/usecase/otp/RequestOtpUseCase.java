package com.aaax.usecase.otp;

import com.aaax.entity.dto.response.RequestOtpResponseDto;
import com.aaax.entity.dto.response.VerifyOtpResponseDto;

import org.springframework.stereotype.Component;

/** OTP request entry (delegates domain ops). */
@Component
public class RequestOtpUseCase {

    private final OtpOpsUseCase otp;

    public RequestOtpUseCase(OtpOpsUseCase otp) {
        this.otp = otp;
    }

    public RequestOtpResponseDto execute(String username) {
        return otp.request(username);
    }

    public VerifyOtpResponseDto verify(String username, String code) {
        return otp.verify(username, code);
    }
}
