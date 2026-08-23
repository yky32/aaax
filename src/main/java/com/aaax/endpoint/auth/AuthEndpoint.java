package com.aaax.endpoint.auth;

import java.security.Principal;
import java.util.Map;

import com.aaax.entity.dto.response.GetAccountResponseDto;
import com.aaax.entity.dto.request.BootstrapAdminRequestDto;
import com.aaax.entity.dto.request.TotpCodeRequestDto;
import com.aaax.usecase.account.AccountQueries;
import com.aaax.usecase.account.BootstrapAdminUseCase;
import com.aaax.usecase.auth.CompleteTotpLoginUseCase;
import com.aaax.usecase.auth.LogoutUseCase;
import com.aaax.usecase.auth.MagicLinkUseCase;
import com.aaax.usecase.auth.MagicLinkUseCase.ConsumeCommand;
import com.aaax.usecase.auth.MagicLinkUseCase.RequestCommand;
import com.aaax.usecase.auth.OtpLoginUseCase;
import com.aaax.usecase.auth.OtpLoginUseCase.OtpLoginCommand;
import com.aaax.usecase.auth.PasswordLoginUseCase;
import com.aaax.usecase.auth.PasswordLoginUseCase.PasswordLoginCommand;
import com.aaax.usecase.auth.QrLoginUseCase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthEndpoint {

    private final AccountQueries accountQueries;
    private final BootstrapAdminUseCase bootstrapAdminUseCase;
    private final PasswordLoginUseCase passwordLoginUseCase;
    private final CompleteTotpLoginUseCase completeTotpLoginUseCase;
    private final OtpLoginUseCase otpLoginUseCase;
    private final MagicLinkUseCase magicLinkUseCase;
    private final QrLoginUseCase qrLoginUseCase;
    private final LogoutUseCase logoutUseCase;

    public AuthEndpoint(
            AccountQueries accountQueries,
            BootstrapAdminUseCase bootstrapAdminUseCase,
            PasswordLoginUseCase passwordLoginUseCase,
            CompleteTotpLoginUseCase completeTotpLoginUseCase,
            OtpLoginUseCase otpLoginUseCase,
            MagicLinkUseCase magicLinkUseCase,
            QrLoginUseCase qrLoginUseCase,
            LogoutUseCase logoutUseCase) {
        this.accountQueries = accountQueries;
        this.bootstrapAdminUseCase = bootstrapAdminUseCase;
        this.passwordLoginUseCase = passwordLoginUseCase;
        this.completeTotpLoginUseCase = completeTotpLoginUseCase;
        this.otpLoginUseCase = otpLoginUseCase;
        this.magicLinkUseCase = magicLinkUseCase;
        this.qrLoginUseCase = qrLoginUseCase;
        this.logoutUseCase = logoutUseCase;
    }

    @GetMapping("/bootstrap/status")
    public Map<String, Object> bootstrapStatus() {
        return Map.of(
                "needsBootstrap", accountQueries.needsBootstrap(),
                "tokenRequired", bootstrapAdminUseCase.tokenRequired());
    }

    @PostMapping("/bootstrap/admin")
    @ResponseStatus(HttpStatus.CREATED)
    public GetAccountResponseDto bootstrap(@Valid @RequestBody BootstrapAdminRequestDto body) {
        return bootstrapAdminUseCase.execute(body);
    }

    @PostMapping("/login")
    public Map<String, Object> login(
            @Valid @RequestBody PasswordLoginCommand body,
            HttpServletRequest request,
            HttpServletResponse response) {
        return passwordLoginUseCase.execute(body, request, response);
    }

    @PostMapping("/mfa/totp")
    public Map<String, Object> completeTotp(
            @Valid @RequestBody TotpCodeRequestDto body,
            HttpServletRequest request,
            HttpServletResponse response) {
        return completeTotpLoginUseCase.execute(body, request, response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, Principal principal) {
        logoutUseCase.execute(request, principal);
    }

    @PostMapping("/otp/login")
    public Map<String, Object> otpLoginUseCase(
            @Valid @RequestBody OtpLoginCommand body,
            HttpServletRequest request,
            HttpServletResponse response) {
        return otpLoginUseCase.execute(body, request, response);
    }

    @PostMapping("/magic/request")
    public Map<String, Object> magicRequest(@Valid @RequestBody RequestCommand body) {
        return magicLinkUseCase.request(body);
    }

    @PostMapping("/magic/consume")
    public Map<String, Object> magicConsume(
            @Valid @RequestBody ConsumeCommand body,
            HttpServletRequest request,
            HttpServletResponse response) {
        return magicLinkUseCase.consume(body, request, response);
    }

    @GetMapping("/magic/consume")
    public Map<String, Object> magicConsumeGet(
            @RequestParam String token, HttpServletRequest request, HttpServletResponse response) {
        return magicLinkUseCase.consume(new ConsumeCommand(token), request, response);
    }

    // --- QR login (desktop pending → phone approve → desktop consume) ---

    @PostMapping("/qr/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> qrCreate() {
        return qrLoginUseCase.create();
    }

    @GetMapping("/qr/sessions/{id}")
    public Map<String, Object> qrStatus(@PathVariable String id) {
        return qrLoginUseCase.status(id);
    }

    @PostMapping("/qr/sessions/{id}/approve")
    public Map<String, Object> qrApprove(@PathVariable String id, Principal principal) {
        return qrLoginUseCase.approve(id, principal);
    }

    @PostMapping("/qr/approve-code")
    public Map<String, Object> qrApproveCode(@Valid @RequestBody QrCodeBody body, Principal principal) {
        return qrLoginUseCase.approveByCode(body.userCode(), principal);
    }

    @PostMapping("/qr/sessions/{id}/consume")
    public Map<String, Object> qrConsume(
            @PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
        return qrLoginUseCase.consume(id, request, response);
    }

    public record QrCodeBody(@NotBlank String userCode) {
    }
}
