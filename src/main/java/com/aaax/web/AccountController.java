package com.aaax.web;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import com.aaax.account.AccountResponse;
import com.aaax.account.AccountService;
import com.aaax.account.AccountService.ChangePasswordRequest;
import com.aaax.account.AccountService.ForgotPasswordRequest;
import com.aaax.account.AccountService.ResetPasswordRequest;
import com.aaax.account.RegisterAccountRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse register(@Valid @RequestBody RegisterAccountRequest request) {
        return accountService.register(request);
    }

    @GetMapping("/me")
    public AccountResponse me(Principal principal) {
        return accountService.requireByUsername(principal.getName());
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(Principal principal, @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(principal.getName(), request.currentPassword(), request.newPassword());
    }

    @PostMapping("/password/forgot")
    public Map<String, Object> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        accountService.requestPasswordReset(request.usernameOrEmail());
        return Map.of(
                "accepted", true,
                "message", "If the account exists, a reset code was sent");
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetPasswordRequest request) {
        accountService.resetPassword(request.username(), request.code(), request.newPassword());
    }
}
