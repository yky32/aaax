package com.aaax.endpoint.compat;

import java.util.Map;

import com.aaax.entity.dto.request.ForgotPasswordRequestDto;
import com.aaax.entity.dto.request.RegisterAccountRequestDto;
import com.aaax.entity.dto.request.RequestOtpRequestDto;
import com.aaax.entity.dto.request.ResetPasswordRequestDto;
import com.aaax.entity.dto.request.VerifyOtpRequestDto;
import com.aaax.entity.dto.response.GetAccountResponseDto;
import com.aaax.entity.dto.response.RequestOtpResponseDto;
import com.aaax.entity.dto.response.VerifyOtpResponseDto;
import com.aaax.usecase.account.PasswordUseCase;
import com.aaax.usecase.account.RegisterAccountUseCase;
import com.aaax.usecase.otp.RequestOtpUseCase;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin compatibility surface inspired by qs/uaa public paths.
 */
@RestController
public class UaaCompatEndpoint {

    private final RegisterAccountUseCase registerAccountUseCase;
    private final PasswordUseCase passwordUseCase;
    private final RequestOtpUseCase requestOtpUseCase;

    public UaaCompatEndpoint(
            RegisterAccountUseCase registerAccountUseCase, PasswordUseCase passwordUseCase, RequestOtpUseCase requestOtpUseCase) {
        this.registerAccountUseCase = registerAccountUseCase;
        this.passwordUseCase = passwordUseCase;
        this.requestOtpUseCase = requestOtpUseCase;
    }

    @PostMapping("/users/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public GetAccountResponseDto register(@Valid @RequestBody RegisterAccountRequestDto request) {
        return registerAccountUseCase.execute(request);
    }

    @PostMapping("/authentications/one-time-passwords/general")
    public RequestOtpResponseDto otpIssue(@Valid @RequestBody RequestOtpRequestDto body) {
        return requestOtpUseCase.execute(body.username());
    }

    @PostMapping("/authentications/one-time-passwords/general/verifications")
    public VerifyOtpResponseDto otpVerify(@Valid @RequestBody VerifyOtpRequestDto body) {
        return requestOtpUseCase.verify(body.username(), body.code());
    }

    @PostMapping("/users/credentials/reset")
    public Map<String, Object> forgot(@Valid @RequestBody ForgotPasswordRequestDto request) {
        passwordUseCase.requestPasswordReset(request.usernameOrEmail());
        return Map.of("accepted", true);
    }

    @PutMapping("/users/credentials/reset/one-time-passwords")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetPasswordRequestDto request) {
        passwordUseCase.resetPassword(request.username(), request.code(), request.newPassword());
    }
}
