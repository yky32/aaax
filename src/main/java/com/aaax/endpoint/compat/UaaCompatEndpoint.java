package com.aaax.endpoint.compat;

import java.util.Map;

import com.aaax.entity.dto.response.AccountResponse;
import com.aaax.entity.dto.request.RegisterAccountRequest;
import com.aaax.entity.dto.AccountDtos.ForgotPasswordRequest;
import com.aaax.entity.dto.AccountDtos.ResetPasswordRequest;
import com.aaax.usecase.account.PasswordUseCase;
import com.aaax.usecase.account.RegisterAccountUseCase;
import com.aaax.entity.dto.response.OtpRequestResponse;
import com.aaax.entity.dto.response.OtpVerifyResponse;
import com.aaax.usecase.otp.RequestOtpUseCase;
import com.aaax.endpoint.otp.OtpEndpoint.OtpVerifyBody;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.aaax.entity.dto.AccountDtos;

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
    public AccountResponse register(@Valid @RequestBody RegisterAccountRequest request) {
        return registerAccount.execute(request);
    }

    @PostMapping("/authentications/one-time-passwords/general")
    public OtpRequestResponse otpIssue(@Valid @RequestBody OtpUserBody body) {
        return requestOtp.execute(body.username());
    }

    @PostMapping("/authentications/one-time-passwords/general/verifications")
    public OtpVerifyResponse otpVerify(@Valid @RequestBody OtpVerifyBody body) {
        return requestOtp.verify(body.username(), body.code());
    }

    @PostMapping("/users/credentials/reset")
    public Map<String, Object> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        passwords.requestPasswordReset(request.usernameOrEmail());
        return Map.of("accepted", true);
    }

    @PutMapping("/users/credentials/reset/one-time-passwords")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetPasswordRequest request) {
        passwords.resetPassword(request.username(), request.code(), request.newPassword());
    }

    public record OtpUserBody(@NotBlank @Size(max = 64) String username) {
    }
}
