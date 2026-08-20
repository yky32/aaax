package com.aaax.web;

import java.security.Principal;

import com.aaax.account.AccountResponse;
import com.aaax.account.application.AccountDtos.DisableTotpRequest;
import com.aaax.account.application.AccountDtos.TotpCodeRequest;
import com.aaax.account.application.AccountDtos.TotpSetupResponse;
import com.aaax.account.application.TotpMfaUseCase;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/accounts/me/mfa")
public class MfaController {

    private final TotpMfaUseCase totpMfa;

    public MfaController(TotpMfaUseCase totpMfa) {
        this.totpMfa = totpMfa;
    }

    @PostMapping("/totp/setup")
    public TotpSetupResponse setup(Principal principal) {
        return totpMfa.beginSetup(principal.getName());
    }

    @PostMapping("/totp/confirm")
    public AccountResponse confirm(Principal principal, @Valid @RequestBody TotpCodeRequest body) {
        return totpMfa.confirm(principal.getName(), body.code());
    }

    @PostMapping("/totp/disable")
    public AccountResponse disable(Principal principal, @Valid @RequestBody DisableTotpRequest body) {
        return totpMfa.disable(principal.getName(), body.password(), body.code());
    }
}
