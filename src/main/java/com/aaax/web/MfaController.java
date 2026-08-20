package com.aaax.web;

import java.security.Principal;

import com.aaax.account.AccountResponse;
import com.aaax.account.AccountService;
import com.aaax.account.AccountService.DisableTotpRequest;
import com.aaax.account.AccountService.TotpCodeRequest;
import com.aaax.account.AccountService.TotpSetupResponse;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/accounts/me/mfa")
public class MfaController {

    private final AccountService accountService;

    public MfaController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/totp/setup")
    public TotpSetupResponse setup(Principal principal) {
        return accountService.beginTotpSetup(principal.getName());
    }

    @PostMapping("/totp/confirm")
    public AccountResponse confirm(Principal principal, @Valid @RequestBody TotpCodeRequest body) {
        return accountService.confirmTotp(principal.getName(), body.code());
    }

    @PostMapping("/totp/disable")
    public AccountResponse disable(Principal principal, @Valid @RequestBody DisableTotpRequest body) {
        return accountService.disableTotp(principal.getName(), body.password(), body.code());
    }
}
