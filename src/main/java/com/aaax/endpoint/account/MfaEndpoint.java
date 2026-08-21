package com.aaax.endpoint.account;

import java.security.Principal;

import com.aaax.entity.dto.response.AccountResponse;
import com.aaax.entity.dto.AccountDtos.DisableTotpRequest;
import com.aaax.entity.dto.AccountDtos.TotpCodeRequest;
import com.aaax.entity.dto.AccountDtos.TotpSetupResponse;
import com.aaax.usecase.account.TotpMfaUseCase;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.aaax.entity.dto.AccountDtos;

@RestController
@RequestMapping("/v1/accounts/me/mfa")
public class MfaEndpoint {

    private final TotpMfaUseCase totpMfa;

    public MfaEndpoint(TotpMfaUseCase totpMfa) {
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
