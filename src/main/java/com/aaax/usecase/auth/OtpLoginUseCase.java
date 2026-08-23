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

    private final OtpOpsUseCase otpOpsUseCase;
    private final FinishAuthenticatedSession finishAuthenticatedSession;

    public OtpLoginUseCase(OtpOpsUseCase otpOpsUseCase, FinishAuthenticatedSession finishAuthenticatedSession) {
        this.otpOpsUseCase = otpOpsUseCase;
        this.finishAuthenticatedSession = finishAuthenticatedSession;
    }

    public Map<String, Object> execute(
            OtpLoginCommand cmd, HttpServletRequest request, HttpServletResponse response) {
        return finishAuthenticatedSession.execute(otpOpsUseCase.verifyForLogin(cmd.username(), cmd.code()), "otp", request, response, true);
    }

    public record OtpLoginCommand(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(min = 4, max = 10) String code
    ) {
    }
}
