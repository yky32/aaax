package com.aaax.web;

import java.util.Map;

import com.aaax.account.AccountResponse;
import com.aaax.account.application.AccountDtos.BootstrapAdminRequest;
import com.aaax.account.application.AccountDtos.TotpCodeRequest;
import com.aaax.account.application.AccountQueries;
import com.aaax.account.application.BootstrapAdminUseCase;
import com.aaax.auth.application.CompleteTotpLoginUseCase;
import com.aaax.auth.application.LogoutUseCase;
import com.aaax.auth.application.MagicLinkUseCase;
import com.aaax.auth.application.MagicLinkUseCase.ConsumeCommand;
import com.aaax.auth.application.MagicLinkUseCase.RequestCommand;
import com.aaax.auth.application.OtpLoginUseCase;
import com.aaax.auth.application.OtpLoginUseCase.OtpLoginCommand;
import com.aaax.auth.application.PasswordLoginUseCase;
import com.aaax.auth.application.PasswordLoginUseCase.PasswordLoginCommand;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AccountQueries accountQueries;
    private final BootstrapAdminUseCase bootstrapAdmin;
    private final PasswordLoginUseCase passwordLogin;
    private final CompleteTotpLoginUseCase completeTotpLogin;
    private final OtpLoginUseCase otpLogin;
    private final MagicLinkUseCase magicLink;
    private final LogoutUseCase logoutUseCase;

    public AuthController(
            AccountQueries accountQueries,
            BootstrapAdminUseCase bootstrapAdmin,
            PasswordLoginUseCase passwordLogin,
            CompleteTotpLoginUseCase completeTotpLogin,
            OtpLoginUseCase otpLogin,
            MagicLinkUseCase magicLink,
            LogoutUseCase logoutUseCase) {
        this.accountQueries = accountQueries;
        this.bootstrapAdmin = bootstrapAdmin;
        this.passwordLogin = passwordLogin;
        this.completeTotpLogin = completeTotpLogin;
        this.otpLogin = otpLogin;
        this.magicLink = magicLink;
        this.logoutUseCase = logoutUseCase;
    }

    @GetMapping("/bootstrap/status")
    public Map<String, Object> bootstrapStatus() {
        return Map.of(
                "needsBootstrap", accountQueries.needsBootstrap(),
                "tokenRequired", bootstrapAdmin.tokenRequired());
    }

    @PostMapping("/bootstrap/admin")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse bootstrap(@Valid @RequestBody BootstrapAdminRequest body) {
        return bootstrapAdmin.execute(body);
    }

    @PostMapping("/login")
    public Map<String, Object> login(
            @Valid @RequestBody PasswordLoginCommand body,
            HttpServletRequest request,
            HttpServletResponse response) {
        return passwordLogin.execute(body, request, response);
    }

    @PostMapping("/mfa/totp")
    public Map<String, Object> completeTotp(
            @Valid @RequestBody TotpCodeRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        return completeTotpLogin.execute(body, request, response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, java.security.Principal principal) {
        logoutUseCase.execute(request, principal);
    }

    @PostMapping("/otp/login")
    public Map<String, Object> otpLogin(
            @Valid @RequestBody OtpLoginCommand body,
            HttpServletRequest request,
            HttpServletResponse response) {
        return otpLogin.execute(body, request, response);
    }

    @PostMapping("/magic/request")
    public Map<String, Object> magicRequest(@Valid @RequestBody RequestCommand body) {
        return magicLink.request(body);
    }

    @PostMapping("/magic/consume")
    public Map<String, Object> magicConsume(
            @Valid @RequestBody ConsumeCommand body,
            HttpServletRequest request,
            HttpServletResponse response) {
        return magicLink.consume(body, request, response);
    }

    @GetMapping("/magic/consume")
    public Map<String, Object> magicConsumeGet(
            @RequestParam String token, HttpServletRequest request, HttpServletResponse response) {
        return magicLink.consume(new ConsumeCommand(token), request, response);
    }
}
