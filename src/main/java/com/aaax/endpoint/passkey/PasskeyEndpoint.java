package com.aaax.endpoint.passkey;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import com.aaax.usecase.auth.FinishAuthenticatedSession;
import com.aaax.usecase.passkey.PasskeyFeatures;
import com.aaax.usecase.passkey.PasskeyUseCase;
import com.aaax.usecase.passkey.PasskeyUseCase.AuthenticateRequest;
import com.aaax.usecase.passkey.PasskeyUseCase.RegisterRequest;

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
public class PasskeyEndpoint {

    private final PasskeyUseCase passkeyUseCase;
    private final PasskeyFeatures passkeyFeatures;
    private final FinishAuthenticatedSession finishAuthenticatedSession;

    public PasskeyEndpoint(
            PasskeyUseCase passkeyUseCase, PasskeyFeatures passkeyFeatures, FinishAuthenticatedSession finishAuthenticatedSession) {
        this.passkeyUseCase = passkeyUseCase;
        this.passkeyFeatures = passkeyFeatures;
        this.finishAuthenticatedSession = finishAuthenticatedSession;
    }

    @GetMapping("/register/options")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> registerOptions(Principal principal) {
        passkeyFeatures.requireEnabled();
        return passkeyUseCase.registrationOptions(principal.getName());
    }

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> register(Principal principal, @RequestBody RegisterRequest body) {
        passkeyFeatures.requireEnabled();
        return passkeyUseCase.register(principal.getName(), body);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> list(Principal principal) {
        passkeyFeatures.requireEnabled();
        return passkeyUseCase.list(principal.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable String id) {
        passkeyFeatures.requireEnabled();
        passkeyUseCase.delete(principal.getName(), id);
    }

    @GetMapping("/authenticate/options")
    public Map<String, Object> authOptions(@RequestParam(required = false) String username) {
        passkeyFeatures.requireEnabled();
        return passkeyUseCase.authenticationOptions(username);
    }

    @PostMapping("/authenticate")
    public Map<String, Object> authenticate(
            @Valid @RequestBody AuthenticateRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        passkeyFeatures.requireEnabled();
        return finishAuthenticatedSession.execute(passkeyUseCase.authenticate(body), "passkey", request, response, true);
    }
}
