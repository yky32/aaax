package com.aaax.account.application;

import java.util.UUID;
import java.util.function.Consumer;

import com.aaax.account.Account;
import com.aaax.account.AccountRepository;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class FederateAccountUseCase {

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final IdentityEventBus events;

    public FederateAccountUseCase(
            AccountRepository accounts, PasswordEncoder passwordEncoder, IdentityEventBus events) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
    }

    @Transactional
    public Account linkOrCreateGoogle(String sub, String email, String nameHint) {
        return accounts.findByGoogleSub(sub)
                .or(() -> email != null ? accounts.findByEmailIgnoreCase(email) : java.util.Optional.empty())
                .map(existing -> {
                    if (existing.getGoogleSub() == null) {
                        existing.setGoogleSub(sub);
                        return accounts.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> createFederated(email, nameHint, a -> a.setGoogleSub(sub), "google:" + sub));
    }

    @Transactional
    public Account linkOrCreateGithub(String githubId, String email, String login) {
        return accounts.findByGithubId(githubId)
                .or(() -> email != null ? accounts.findByEmailIgnoreCase(email) : java.util.Optional.empty())
                .map(existing -> {
                    if (existing.getGithubId() == null) {
                        existing.setGithubId(githubId);
                        return accounts.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> createFederated(email, login != null ? login : "github",
                        a -> a.setGithubId(githubId), "github:" + githubId));
    }

    @Transactional
    public Account linkOrCreateSaml(String nameId, String email, String nameHint) {
        return accounts.findBySamlNameId(nameId)
                .or(() -> email != null ? accounts.findByEmailIgnoreCase(email) : java.util.Optional.empty())
                .map(existing -> {
                    if (existing.getSamlNameId() == null) {
                        existing.setSamlNameId(nameId);
                        return accounts.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> createFederated(email, nameHint, a -> a.setSamlNameId(nameId), "saml:" + nameId));
    }

    private Account createFederated(String email, String nameHint, Consumer<Account> linker, String detail) {
        String base = StringUtils.hasText(nameHint) ? nameHint.replaceAll("[^a-zA-Z0-9._-]", "") : "user";
        if (base.length() < 3) {
            base = "user";
        }
        String username = base.substring(0, Math.min(base.length(), 40));
        int i = 0;
        while (accounts.existsByUsernameIgnoreCase(username)) {
            i++;
            username = base.substring(0, Math.min(base.length(), 36)) + i;
        }
        Account created = new Account(
                username,
                RegisterAccountUseCase.normalizeEmail(email),
                passwordEncoder.encode(UUID.randomUUID().toString()),
                "USER");
        linker.accept(created);
        Account saved = accounts.save(created);
        events.emit(IdentityEvent.Types.ACCOUNT_FEDERATED, username, detail,
                java.util.Map.of("federation", detail == null ? "" : detail));
        return saved;
    }
}
