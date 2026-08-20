package com.aaax.web;

import com.aaax.otp.OtpRequestResponse;
import com.aaax.otp.OtpService;
import com.aaax.otp.OtpVerifyResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/request")
    public OtpRequestResponse request(@Valid @RequestBody OtpRequestBody body) {
        return otpService.request(body.username());
    }

    @PostMapping("/verify")
    public OtpVerifyResponse verify(@Valid @RequestBody OtpVerifyBody body) {
        return otpService.verify(body.username(), body.code());
    }

    public record OtpRequestBody(
            @NotBlank @Size(max = 64) String username
    ) {
    }

    public record OtpVerifyBody(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(min = 4, max = 10) String code
    ) {
    }
}
