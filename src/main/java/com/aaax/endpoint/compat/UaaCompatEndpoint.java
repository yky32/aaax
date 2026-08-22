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

    private final RegisterAccountUseCase registerAccount;
    private final PasswordUseCase passwords;
    private final RequestOtpUseCase requestOtp;

    public UaaCompatEndpoint(
            RegisterAccountUseCase registerAccount, PasswordUseCase passwords, RequestOtpUseCase requestOtp) {
        this.registerAccount = registerAccount;
        this.passwords = passwords;
        this.requestOtp = requestOtp;
    }

    @PostMapping("/users/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public GetAccountResponseDto register(@Valid @RequestBody RegisterAccountRequestDto request) {
        return registerAccount.execute(request);
    }

    @PostMapping("/authentications/one-time-passwords/general")
    public RequestOtpResponseDto otpIssue(@Valid @RequestBody RequestOtpRequestDto body) {
        return requestOtp.execute(body.username());
    }

    @PostMapping("/authentications/one-time-passwords/general/verifications")
    public VerifyOtpResponseDto otpVerify(@Valid @RequestBody VerifyOtpRequestDto body) {
        return requestOtp.verify(body.username(), body.code());
    }

    @PostMapping("/users/credentials/reset")
    public Map<String, Object> forgot(@Valid @RequestBody ForgotPasswordRequestDto request) {
        passwords.requestPasswordReset(request.usernameOrEmail());
        return Map.of("accepted", true);
    }

    @PutMapping("/users/credentials/reset/one-time-passwords")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetPasswordRequestDto request) {
        passwords.resetPassword(request.username(), request.code(), request.newPassword());
    }
}
