package com.aaax.usecase.auth;

import java.util.Map;

import com.aaax.usecase.otp.OtpOpsUseCase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.stereotype.Component;

@Component
public class OtpLoginUseCase {

    private final OtpOpsUseCase otpService;
    private final FinishAuthenticatedSession finish;

    public OtpLoginUseCase(OtpOpsUseCase otpService, FinishAuthenticatedSession finish) {
        this.otpService = otpService;
        this.finish = finish;
    }

    public Map<String, Object> execute(
            OtpLoginCommand cmd, HttpServletRequest request, HttpServletResponse response) {
        return finish.execute(otpService.verifyForLogin(cmd.username(), cmd.code()), "otp", request, response, true);
    }

    public record OtpLoginCommand(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(min = 4, max = 10) String code
    ) {
    }
}
