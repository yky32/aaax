package com.aaax.compat;

import java.util.Map;

import com.aaax.account.AccountResponse;
import com.aaax.account.AccountService;
import com.aaax.account.AccountService.ForgotPasswordRequest;
import com.aaax.account.AccountService.ResetPasswordRequest;
import com.aaax.account.RegisterAccountRequest;
import com.aaax.otp.OtpRequestResponse;
import com.aaax.otp.OtpService;
import com.aaax.otp.OtpVerifyResponse;
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
 * Not a 1:1 dump — only core identity flows without Quinsic-only deps.
 */
@RestController
public class UaaCompatController {

    private final AccountService accountService;
    private final OtpService otpService;

    public UaaCompatController(AccountService accountService, OtpService otpService) {
        this.accountService = accountService;
        this.otpService = otpService;
    }

    /** Aligns with qs/uaa public registration entry. */
    @PostMapping("/users/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse register(@Valid @RequestBody RegisterAccountRequest request) {
        return accountService.register(request);
    }

    /** Aligns with qs/uaa general OTP issue. */
    @PostMapping("/authentications/one-time-passwords/general")
    public OtpRequestResponse otpIssue(@Valid @RequestBody OtpUserBody body) {
        return otpService.request(body.username());
    }

    @PostMapping("/authentications/one-time-passwords/general/verifications")
    public OtpVerifyResponse otpVerify(@Valid @RequestBody OtpVerifyBody body) {
        return otpService.verify(body.username(), body.code());
    }

    @PostMapping("/users/credentials/reset")
    public Map<String, Object> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        accountService.requestPasswordReset(request.usernameOrEmail());
        return Map.of("accepted", true);
    }

    @PutMapping("/users/credentials/reset/one-time-passwords")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetPasswordRequest request) {
        accountService.resetPassword(request.username(), request.code(), request.newPassword());
    }

    public record OtpUserBody(@NotBlank @Size(max = 64) String username) {
    }
}
