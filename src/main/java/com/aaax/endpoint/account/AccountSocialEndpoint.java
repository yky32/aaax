package com.aaax.endpoint.account;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

import com.aaax.config.SocialProviders;
import com.aaax.entity.dto.response.GetAccountResponseDto;
import com.aaax.entity.po.Account;
import com.aaax.repository.AccountRepository;
import com.aaax.usecase.account.AccountQueries;
import com.aaax.usecase.account.FederateAccountUseCase;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Link / unlink social providers for the signed-in account.
 * Start link: {@code /oauth2/authorization/{id}?aaax_link=1&aaax_return=/user/}
 */
@RestController
@RequestMapping("/v1/accounts/me/social")
@PreAuthorize("isAuthenticated()")
public class AccountSocialEndpoint {

    private final FederateAccountUseCase federateAccountUseCase;
    private final AccountQueries accountQueries;
    private final AccountRepository accountRepository;
    private final SocialProviders socialProviders;

    public AccountSocialEndpoint(
            FederateAccountUseCase federateAccountUseCase,
            AccountQueries accountQueries,
            AccountRepository accountRepository,
            SocialProviders socialProviders) {
        this.federateAccountUseCase = federateAccountUseCase;
        this.accountQueries = accountQueries;
        this.accountRepository = accountRepository;
        this.socialProviders = socialProviders;
    }

    @GetMapping
    public Map<String, Object> status(Principal principal) {
        Account a = accountRepository
                .findByUsernameIgnoreCase(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("linkedProviders", federateAccountUseCase.linkedProvidersList(a));
        body.put("googleLinked", federateAccountUseCase.linkedProviders(a).contains("google"));
        body.put("githubLinked", federateAccountUseCase.linkedProviders(a).contains("github"));
        body.put("providers", socialProviders.toPublicBody().get("providers"));
        body.put("enabled", socialProviders.anyEnabled());
        body.put("supportedCatalog", SocialProviders.CATALOG.stream().map(SocialProviders.ProviderDef::id).toList());
        return body;
    }

    @DeleteMapping("/{provider}")
    public GetAccountResponseDto unlink(Principal principal, @PathVariable String provider) {
        Account saved = federateAccountUseCase.unlink(principal.getName(), provider);
        return accountQueries.requireByUsername(saved.getUsername());
    }
}
