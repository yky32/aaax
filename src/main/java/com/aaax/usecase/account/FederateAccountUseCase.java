package com.aaax.usecase.account;

import com.aaax.exception.response.AccountErrorResponse;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.aaax.config.SocialProviders;
import com.aaax.core.exception.BizException;
import com.aaax.exception.response.SocialErrorResponse;
import com.aaax.entity.po.account.Account;
import com.aaax.entity.po.account.AccountSocialLink;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;
import com.aaax.repository.AccountRepository;
import com.aaax.repository.AccountSocialLinkRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Federate / link / unlink social identities — qs/uaa style (identity rows, not columns on Account).
 */
@Component
public class FederateAccountUseCase {

    private final AccountRepository accountRepository;
    private final AccountSocialLinkRepository accountSocialLinkRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityEventBus identityEventBus;

    public FederateAccountUseCase(
            AccountRepository accountRepository,
            AccountSocialLinkRepository accountSocialLinkRepository,
            PasswordEncoder passwordEncoder,
            IdentityEventBus identityEventBus) {
        this.accountRepository = accountRepository;
        this.accountSocialLinkRepository = accountSocialLinkRepository;
        this.passwordEncoder = passwordEncoder;
        this.identityEventBus = identityEventBus;
    }

    @Transactional
    public Account linkOrCreate(String provider, String externalId, String email, String nameHint) {
        String p = requireProvider(provider);
        String ext = requireExternal(externalId);

        Optional<AccountSocialLink> byExt = accountSocialLinkRepository.findByProviderAndExternalId(p, ext);
        if (byExt.isPresent()) {
            return requireAccountId(byExt.get().getAccountId());
        }

        if (StringUtils.hasText(email)) {
            Optional<Account> byEmail = accountRepository.findByEmailIgnoreCase(email.trim());
            if (byEmail.isPresent()) {
                return attach(byEmail.get(), p, ext);
            }
        }

        return createFederated(email, nameHint, p, ext);
    }

    @Transactional
    public Account linkToUsername(String username, String provider, String externalId) {
        String p = requireProvider(provider);
        String ext = requireExternal(externalId);
        Account account = requireUser(username);

        Optional<AccountSocialLink> owner = accountSocialLinkRepository.findByProviderAndExternalId(p, ext);
        if (owner.isPresent() && !owner.get().getAccountId().equals(account.getId())) {
            throw new BizException(SocialErrorResponse.SOC0001, "provider=" + p);
        }
        return attach(account, p, ext);
    }

    @Transactional
    public Account unlink(String username, String provider) {
        String p = requireProvider(provider);
        Account account = requireUser(username);
        Optional<AccountSocialLink> link = accountSocialLinkRepository.findByAccountIdAndProvider(account.getId(), p);
        if (link.isEmpty()) {
            throw new BizException(SocialErrorResponse.SOC0002, p + " is not linked");
        }
        ensureCanUnlink(account, p);
        accountSocialLinkRepository.delete(link.get());
        identityEventBus.emit(
                IdentityEvent.Types.ACCOUNT_SOCIAL_UNLINKED, username, p, Map.of("provider", p));
        return account;
    }

    @Transactional
    public Account linkOrCreateGoogle(String sub, String email, String nameHint) {
        return linkOrCreate("google", sub, email, nameHint);
    }

    @Transactional
    public Account linkOrCreateGithub(String githubId, String email, String login) {
        return linkOrCreate("github", githubId, email, login);
    }

    @Transactional
    public Account linkGoogleToUsername(String username, String sub) {
        return linkToUsername(username, "google", sub);
    }

    @Transactional
    public Account linkGithubToUsername(String username, String githubId) {
        return linkToUsername(username, "github", githubId);
    }

    @Transactional
    public Account unlinkGoogle(String username) {
        return unlink(username, "google");
    }

    @Transactional
    public Account unlinkGithub(String username) {
        return unlink(username, "github");
    }

    @Transactional
    public Account linkOrCreateSaml(String nameId, String email, String nameHint) {
        return accountRepository
                .findBySamlNameId(nameId)
                .or(() -> StringUtils.hasText(email) ? accountRepository.findByEmailIgnoreCase(email) : Optional.empty())
                .map(existing -> {
                    if (existing.getSamlNameId() == null) {
                        existing.setSamlNameId(nameId);
                        return accountRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Account created = newBlank(email, nameHint);
                    created.setSamlNameId(nameId);
                    Account saved = accountRepository.save(created);
                    identityEventBus.emit(
                            IdentityEvent.Types.ACCOUNT_FEDERATED,
                            saved.getUsername(),
                            "saml:" + nameId,
                            Map.of("provider", "saml", "externalId", nameId));
                    return saved;
                });
    }

    public Set<String> linkedProviders(Account account) {
        Set<String> set = new LinkedHashSet<>();
        for (AccountSocialLink l : accountSocialLinkRepository.findByAccountIdOrderByCreateDtAsc(account.getId())) {
            if (Boolean.TRUE.equals(l.getIsActive()) || l.getIsActive() == null) {
                set.add(l.getProvider());
            }
        }
        return set;
    }

    public List<String> linkedProvidersList(Account account) {
        return linkedProviders(account).stream().sorted().collect(Collectors.toList());
    }

    private Account attach(Account account, String provider, String externalId) {
        Optional<AccountSocialLink> existing =
                accountSocialLinkRepository.findByAccountIdAndProvider(account.getId(), provider);
        if (existing.isPresent()) {
            if (!existing.get().getExternalId().equals(externalId)) {
                throw new BizException(SocialErrorResponse.SOC0006, "provider=" + provider);
            }
            return account;
        }
        accountSocialLinkRepository.save(new AccountSocialLink(account.getId(), provider, externalId));
        identityEventBus.emit(
                IdentityEvent.Types.ACCOUNT_SOCIAL_LINKED,
                account.getUsername(),
                provider,
                Map.of("provider", provider));
        return account;
    }

    private void ensureCanUnlink(Account account, String unlinkingProvider) {
        Set<String> linked = linkedProviders(account);
        linked.remove(unlinkingProvider);
        if (linked.isEmpty() && !StringUtils.hasText(account.getEmail())) {
            throw new BizException(SocialErrorResponse.SOC0005, "Link another method or set an email before unlinking the last social login");
        }
    }

    private Account createFederated(String email, String nameHint, String provider, String externalId) {
        Account saved = accountRepository.save(newBlank(email, nameHint));
        accountSocialLinkRepository.save(new AccountSocialLink(saved.getId(), provider, externalId));
        identityEventBus.emit(
                IdentityEvent.Types.ACCOUNT_FEDERATED,
                saved.getUsername(),
                provider + ":" + externalId,
                Map.of("provider", provider, "externalId", externalId));
        return saved;
    }

    private Account newBlank(String email, String nameHint) {
        String base = StringUtils.hasText(nameHint) ? nameHint.replaceAll("[^a-zA-Z0-9._-]", "") : "user";
        if (base.length() < 3) {
            base = "user";
        }
        String username = base.substring(0, Math.min(base.length(), 40)).toLowerCase(Locale.ROOT);
        int i = 0;
        while (accountRepository.existsByUsernameIgnoreCase(username)) {
            i++;
            username = base.substring(0, Math.min(base.length(), 36)).toLowerCase(Locale.ROOT) + i;
        }
        return new Account(
                username,
                RegisterAccountUseCase.normalizeEmail(email),
                passwordEncoder.encode(UUID.randomUUID().toString()),
                "USER");
    }

    private Account requireUser(String username) {
        return accountRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0001, "account not found"));
    }

    private Account requireAccountId(String id) {
        return accountRepository.findById(id).orElseThrow(() -> new BizException(AccountErrorResponse.ACC0001, "account not found"));
    }

    private static String requireProvider(String provider) {
        String p = SocialProviders.normalize(provider);
        if (!SocialProviders.KNOWN_IDS.contains(p) && !"saml".equals(p)) {
            throw new BizException(SocialErrorResponse.SOC0003, "Unknown social provider: " + provider);
        }
        return p;
    }

    private static String requireExternal(String externalId) {
        if (!StringUtils.hasText(externalId)) {
            throw new BizException(SocialErrorResponse.SOC0004, "Provider subject / id is required");
        }
        return externalId.trim();
    }
}
