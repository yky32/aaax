package com.aaax.web;

import java.util.LinkedHashMap;
import java.util.Map;

import com.aaax.account.Account;
import com.aaax.account.AccountResponse;
import com.aaax.account.AccountService;
import com.aaax.account.AccountService.BootstrapAdminRequest;
import com.aaax.account.AccountService.DisableTotpRequest;
import com.aaax.account.AccountService.TotpCodeRequest;
import com.aaax.account.AccountService.TotpSetupResponse;
import com.aaax.account.AccountUserDetailsService;
import com.aaax.audit.AuditService;
import com.aaax.otp.OtpService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    public static final String MFA_PENDING_USER = "AAAX_MFA_PENDING_USER";

    private final OtpService otpService;
    private final AccountService accountService;
    private final AccountUserDetailsService userDetailsService;
    private final AuditService auditService;
    private final String bootstrapToken;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(
            OtpService otpService,
            AccountService accountService,
            AccountUserDetailsService userDetailsService,
            AuditService auditService,
            @Value("${aaax.bootstrap.token:}") String bootstrapToken) {
        this.otpService = otpService;
        this.accountService = accountService;
        this.userDetailsService = userDetailsService;
        this.auditService = auditService;
        this.bootstrapToken = bootstrapToken;
    }

    @GetMapping("/bootstrap/status")
    public Map<String, Object> bootstrapStatus() {
        return Map.of(
                "needsBootstrap", accountService.needsBootstrap(),
                "tokenRequired", bootstrapToken != null && !bootstrapToken.isBlank());
    }

    @PostMapping("/bootstrap/admin")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse bootstrap(@Valid @RequestBody BootstrapAdminRequest body) {
        return accountService.bootstrapAdmin(
                body.username(), body.email(), body.password(), body.bootstrapToken(), bootstrapToken);
    }

    @PostMapping("/login")
    public Map<String, Object> login(
            @Valid @RequestBody PasswordLoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        Account account = accountService.authenticatePassword(body.username(), body.password());
        if (account.isTotpEnabled()) {
            HttpSession session = request.getSession(true);
            session.setAttribute(MFA_PENDING_USER, account.getUsername());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("mfaRequired", true);
            m.put("mfaType", "totp");
            m.put("username", account.getUsername());
            return m;
        }
        establishSession(account.getUsername(), request, response);
        auditService.record("auth.login", account.getUsername(), "password");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mfaRequired", false);
        m.put("account", AccountResponse.from(account));
        return m;
    }

    @PostMapping("/mfa/totp")
    public AccountResponse completeTotp(
            @Valid @RequestBody TotpCodeRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(MFA_PENDING_USER) == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "no pending mfa login");
        }
        String username = session.getAttribute(MFA_PENDING_USER).toString();
        if (!accountService.verifyTotp(username, body.code())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "invalid totp code");
        }
        session.removeAttribute(MFA_PENDING_USER);
        Account account = accountService.requireEntityByUsername(username);
        establishSession(username, request, response);
        auditService.record("auth.login", username, "password+totp");
        return AccountResponse.from(account);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @PostMapping("/otp/login")
    public AccountResponse otpLogin(
            @Valid @RequestBody OtpLoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        Account account = otpService.verifyForLogin(body.username(), body.code());
        establishSession(account.getUsername(), request, response);
        auditService.record("auth.login", account.getUsername(), "otp");
        return AccountResponse.from(account);
    }

    private void establishSession(String username, HttpServletRequest request, HttpServletResponse response) {
        UserDetails user = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    public record PasswordLoginRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 128) String password
    ) {
    }

    public record OtpLoginRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(min = 4, max = 10) String code
    ) {
    }
}
