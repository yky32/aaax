package com.aaax.usecase.account;

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
import com.aaax.entity.po.Account;
import com.aaax.entity.po.AccountSocialLink;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;
import com.aaax.repository.AccountRepository;
import com.aaax.repository.AccountSocialLinkRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

        // Legacy columns (google / github) — bridge until fully migrated
        Optional<Account> legacy = findLegacy(p, ext);
        if (legacy.isPresent()) {
            ensureLinkRow(legacy.get(), p, ext);
            return legacy.get();
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
            throw BizException.conflict("This " + p + " identity is already linked to another user");
        }
        Optional<Account> legacy = findLegacy(p, ext);
        if (legacy.isPresent() && !legacy.get().getId().equals(account.getId())) {
            throw BizException.conflict("This " + p + " identity is already linked to another user");
        }
        return attach(account, p, ext);
    }

    @Transactional
    public Account unlink(String username, String provider) {
        String p = requireProvider(provider);
        Account account = requireUser(username);
        Optional<AccountSocialLink> link = accountSocialLinkRepository.findByAccountIdAndProvider(account.getId(), p);
        boolean hadLegacy = clearLegacy(account, p);
        if (link.isEmpty() && !hadLegacy) {
            throw BizException.badRequest(p + "_not_linked", p + " is not linked");
        }
        ensureCanUnlink(account, p);
        link.ifPresent(accountSocialLinkRepository::delete);
        if (hadLegacy) {
            accountRepository.save(account);
        }
        identityEventBus.emit(
                IdentityEvent.Types.ACCOUNT_SOCIAL_UNLINKED,
                username,
                p,
                Map.of("provider", p));
        return account;
    }

    /** @deprecated prefer {@link #linkOrCreate} */
    @Transactional
    public Account linkOrCreateGoogle(String sub, String email, String nameHint) {
        return linkOrCreate("google", sub, email, nameHint);
    }

    /** @deprecated prefer {@link #linkOrCreate} */
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
            set.add(l.getProvider());
        }
        if (StringUtils.hasText(account.getGoogleSub())) {
            set.add("google");
        }
        if (StringUtils.hasText(account.getGithubId())) {
            set.add("github");
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
                throw BizException.conflict("Account already linked to a different " + provider + " identity");
            }
            syncLegacy(account, provider, externalId);
            return account;
        }
        accountSocialLinkRepository.save(new AccountSocialLink(account.getId(), provider, externalId));
        syncLegacy(account, provider, externalId);
        accountRepository.save(account);
        identityEventBus.emit(
                IdentityEvent.Types.ACCOUNT_SOCIAL_LINKED,
                account.getUsername(),
                provider,
                Map.of("provider", provider));
        return account;
    }

    private void ensureLinkRow(Account account, String provider, String externalId) {
        if (accountSocialLinkRepository.findByAccountIdAndProvider(account.getId(), provider).isEmpty()) {
            accountSocialLinkRepository.save(new AccountSocialLink(account.getId(), provider, externalId));
        }
    }

    private void syncLegacy(Account account, String provider, String externalId) {
        if ("google".equals(provider)) {
            account.setGoogleSub(externalId);
        } else if ("github".equals(provider)) {
            account.setGithubId(externalId);
        }
    }

    private boolean clearLegacy(Account account, String provider) {
        if ("google".equals(provider) && StringUtils.hasText(account.getGoogleSub())) {
            account.setGoogleSub(null);
            return true;
        }
        if ("github".equals(provider) && StringUtils.hasText(account.getGithubId())) {
            account.setGithubId(null);
            return true;
        }
        return false;
    }

    private Optional<Account> findLegacy(String provider, String externalId) {
        if ("google".equals(provider)) {
            return accountRepository.findByGoogleSub(externalId);
        }
        if ("github".equals(provider)) {
            return accountRepository.findByGithubId(externalId);
        }
        return Optional.empty();
    }

    private void ensureCanUnlink(Account account, String unlinkingProvider) {
        Set<String> linked = linkedProviders(account);
        linked.remove(unlinkingProvider);
        // After unlink, need at least email (password reset) or another social
        if (linked.isEmpty() && !StringUtils.hasText(account.getEmail())) {
            throw BizException.badRequest(
                    "cannot_unlink_last",
                    "Link another method or set an email before unlinking the last social login");
        }
    }

    private Account createFederated(String email, String nameHint, String provider, String externalId) {
        Account created = newBlank(email, nameHint);
        syncLegacy(created, provider, externalId);
        Account saved = accountRepository.save(created);
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
                .orElseThrow(() -> BizException.notFound("Account not found"));
    }

    private Account requireAccountId(String id) {
        return accountRepository.findById(id).orElseThrow(() -> BizException.notFound("Account not found"));
    }

    private static String requireProvider(String provider) {
        String p = SocialProviders.normalize(provider);
        if (!SocialProviders.KNOWN_IDS.contains(p) && !"saml".equals(p)) {
            throw BizException.badRequest("unknown_provider", "Unknown social provider: " + provider);
        }
        return p;
    }

    private static String requireExternal(String externalId) {
        if (!StringUtils.hasText(externalId)) {
            throw BizException.badRequest("external_id_required", "Provider subject / id is required");
        }
        return externalId.trim();
    }
}
