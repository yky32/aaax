package com.aaax.usecase.otp;

import com.aaax.entity.dto.response.OtpRequestResponse;
import com.aaax.entity.dto.response.OtpVerifyResponse;

import org.springframework.stereotype.Component;

/** OTP request entry (delegates domain ops). */
@Component
public class RequestOtpUseCase {

    private final OtpOpsUseCase otp;

    public RequestOtpUseCase(OtpOpsUseCase otp) {
        this.otp = otp;
    }

    public OtpRequestResponse execute(String username) {
        return otp.request(username);
    }

    public OtpVerifyResponse verify(String username, String code) {
        return otp.verify(username, code);
    }
}
