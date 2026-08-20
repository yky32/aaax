package com.aaax.web;

import com.aaax.otp.OtpRequestResponse;
import com.aaax.otp.OtpVerifyResponse;
import com.aaax.otp.application.RequestOtpUseCase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/otp")
public class OtpEndpoint {

    private final RequestOtpUseCase requestOtp;

    public OtpEndpoint(RequestOtpUseCase requestOtp) {
        this.requestOtp = requestOtp;
    }

    @PostMapping("/request")
    public OtpRequestResponse request(@Valid @RequestBody OtpRequestBody body) {
        return requestOtp.execute(body.username());
    }

    @PostMapping("/verify")
    public OtpVerifyResponse verify(@Valid @RequestBody OtpVerifyBody body) {
        return requestOtp.verify(body.username(), body.code());
    }

    public record OtpRequestBody(@NotBlank @Size(max = 64) String username) {
    }

    public record OtpVerifyBody(
            @NotBlank @Size(max = 64) String username, @NotBlank @Size(min = 4, max = 10) String code) {
    }
}
