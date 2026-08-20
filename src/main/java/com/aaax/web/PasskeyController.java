package com.aaax.web;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import com.aaax.auth.application.FinishAuthenticatedSession;
import com.aaax.passkey.PasskeyService;
import com.aaax.passkey.PasskeyService.AuthenticateRequest;
import com.aaax.passkey.PasskeyService.RegisterRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/passkeys")
public class PasskeyController {

    private final PasskeyService passkeys;
    private final FinishAuthenticatedSession finishSession;

    public PasskeyController(PasskeyService passkeys, FinishAuthenticatedSession finishSession) {
        this.passkeys = passkeys;
        this.finishSession = finishSession;
    }

    @GetMapping("/register/options")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> registerOptions(Principal principal) {
        return passkeys.registrationOptions(principal.getName());
    }

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> register(Principal principal, @RequestBody RegisterRequest body) {
        return passkeys.register(principal.getName(), body);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> list(Principal principal) {
        return passkeys.list(principal.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable String id) {
        passkeys.delete(principal.getName(), id);
    }

    @GetMapping("/authenticate/options")
    public Map<String, Object> authOptions(@RequestParam(required = false) String username) {
        return passkeys.authenticationOptions(username);
    }

    @PostMapping("/authenticate")
    public Map<String, Object> authenticate(
            @Valid @RequestBody AuthenticateRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        return finishSession.execute(passkeys.authenticate(body), "passkey", request, response, true);
    }
}
