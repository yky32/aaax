package com.aaax.endpoint.account;

import java.security.Principal;

import com.aaax.entity.dto.request.DisableTotpRequestDto;
import com.aaax.entity.dto.request.TotpCodeRequestDto;
import com.aaax.entity.dto.response.GetAccountResponseDto;
import com.aaax.entity.dto.response.TotpSetupResponseDto;
import com.aaax.usecase.account.TotpMfaUseCase;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/accounts/me/mfa")
public class MfaEndpoint {

    private final TotpMfaUseCase totpMfa;

    public MfaEndpoint(TotpMfaUseCase totpMfa) {
        this.totpMfa = totpMfa;
    }

    @PostMapping("/totp/setup")
    public TotpSetupResponseDto setup(Principal principal) {
        return totpMfa.beginSetup(principal.getName());
    }

    @PostMapping("/totp/confirm")
    public GetAccountResponseDto confirm(Principal principal, @Valid @RequestBody TotpCodeRequestDto body) {
        return totpMfa.confirm(principal.getName(), body.code());
    }

    @PostMapping("/totp/disable")
    public GetAccountResponseDto disable(Principal principal, @Valid @RequestBody DisableTotpRequestDto body) {
        return totpMfa.disable(principal.getName(), body.password(), body.code());
    }
}
