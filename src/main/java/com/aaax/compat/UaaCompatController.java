package com.aaax.compat;

import java.util.Map;

import com.aaax.account.AccountResponse;
import com.aaax.account.RegisterAccountRequest;
import com.aaax.account.application.AccountDtos.ForgotPasswordRequest;
import com.aaax.account.application.AccountDtos.ResetPasswordRequest;
import com.aaax.account.application.PasswordUseCase;
import com.aaax.account.application.RegisterAccountUseCase;
import com.aaax.otp.OtpRequestResponse;
import com.aaax.otp.OtpVerifyResponse;
import com.aaax.otp.application.RequestOtpUseCase;
import com.aaax.web.OtpController.OtpVerifyBody;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
public class UaaCompatController {

    private final RegisterAccountUseCase registerAccount;
    private final PasswordUseCase passwords;
    private final RequestOtpUseCase requestOtp;

    public UaaCompatController(
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
