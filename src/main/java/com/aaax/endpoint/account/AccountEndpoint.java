package com.aaax.endpoint.account;

import java.security.Principal;
import java.util.Map;

import com.aaax.entity.dto.response.AccountResponse;
import com.aaax.entity.dto.request.RegisterAccountRequest;
import com.aaax.entity.dto.AccountDtos.ChangePasswordRequest;
import com.aaax.entity.dto.AccountDtos.ForgotPasswordRequest;
import com.aaax.entity.dto.AccountDtos.ResetPasswordRequest;
import com.aaax.usecase.account.AccountQueries;
import com.aaax.usecase.account.PasswordUseCase;
import com.aaax.usecase.account.RegisterAccountUseCase;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.aaax.entity.dto.AccountDtos;

@RestController
@RequestMapping("/v1/accounts")
public class AccountEndpoint {

    private final RegisterAccountUseCase registerAccount;
    private final AccountQueries queries;
    private final PasswordUseCase passwords;

    public AccountEndpoint(
            RegisterAccountUseCase registerAccount, AccountQueries queries, PasswordUseCase passwords) {
        this.registerAccount = registerAccount;
        this.queries = queries;
        this.passwords = passwords;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse register(@Valid @RequestBody RegisterAccountRequest request) {
        return registerAccount.execute(request);
    }

    @GetMapping("/me")
    public AccountResponse me(Principal principal) {
        return queries.requireByUsername(principal.getName());
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(Principal principal, @Valid @RequestBody ChangePasswordRequest request) {
        passwords.changePassword(principal.getName(), request.currentPassword(), request.newPassword());
    }

    @PostMapping("/password/forgot")
    public Map<String, Object> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        passwords.requestPasswordReset(request.usernameOrEmail());
        return Map.of("accepted", true, "message", "If the account exists, a reset code was sent");
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetPasswordRequest request) {
        passwords.resetPassword(request.username(), request.code(), request.newPassword());
    }
}
