package com.aaax.account;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;
import com.aaax.mfa.TotpService;
import com.aaax.otp.InMemoryOtpStore;
import com.aaax.otp.OtpSender;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final InMemoryOtpStore otpStore;
    private final OtpSender otpSender;
    private final TotpService totpService;
    private final IdentityEventBus events;
    private final int otpTtlSeconds;
    private final int otpLength;
    private final String issuer;

    public AccountService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            InMemoryOtpStore otpStore,
            OtpSender otpSender,
            TotpService totpService,
            IdentityEventBus events,
            @Value("${aaax.otp.ttl-seconds:300}") int otpTtlSeconds,
            @Value("${aaax.otp.length:6}") int otpLength,
            @Value("${aaax.issuer:http://localhost:8081}") String issuer) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpStore = otpStore;
        this.otpSender = otpSender;
        this.totpService = totpService;
        this.events = events;
        this.otpTtlSeconds = otpTtlSeconds;
        this.otpLength = Math.max(4, Math.min(otpLength, 10));
        this.issuer = issuer;
    }

    @Transactional
    public AccountResponse register(RegisterAccountRequest request) {
        String username = request.username().trim();
        String email = normalizeEmail(request.email());
        String password = request.password();

        if (accountRepository.existsByUsernameIgnoreCase(username)) {
            throw AccountException.conflict("username already taken");
        }
        if (email != null && accountRepository.existsByEmailIgnoreCase(email)) {
            throw AccountException.conflict("email already registered");
        }

        Account account = new Account(username, email, passwordEncoder.encode(password));
        Account saved = accountRepository.save(account);
        events.emit(IdentityEvent.Types.ACCOUNT_REGISTERED, username, "self-register",
                java.util.Map.of("email", email == null ? "" : email));
        return AccountResponse.from(saved);
    }

    @Transactional
    public AccountResponse bootstrapAdmin(String username, String email, String password, String bootstrapToken, String configuredToken) {
        if (accountRepository.countByRolesContainingIgnoreCase("ADMIN") > 0) {
            throw AccountException.conflict("admin already exists");
        }
        if (StringUtils.hasText(configuredToken)) {
            if (!configuredToken.equals(bootstrapToken)) {
                throw AccountException.badRequest("invalid bootstrap token");
            }
        }
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password) || password.length() < 8) {
            throw AccountException.badRequest("username and password (min 8) required");
        }
        if (accountRepository.existsByUsernameIgnoreCase(username.trim())) {
            throw AccountException.conflict("username already taken");
        }
        Account account = new Account(
                username.trim(),
                normalizeEmail(email),
                passwordEncoder.encode(password),
                "USER,ADMIN");
        Account saved = accountRepository.save(account);
        events.emit(IdentityEvent.Types.BOOTSTRAP_ADMIN, saved.getUsername(), "first admin",
                java.util.Map.of("roles", "USER,ADMIN"));
        return AccountResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public boolean needsBootstrap() {
        return accountRepository.countByRolesContainingIgnoreCase("ADMIN") == 0;
    }

    @Transactional(readOnly = true)
    public AccountResponse requireByUsername(String username) {
        return AccountResponse.from(requireEntityByUsername(username));
    }

    @Transactional(readOnly = true)
    public Account requireEntityByUsername(String username) {
        return accountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> AccountException.notFound("account not found"));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAll() {
        return accountRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getById(String id) {
        return accountRepository.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> AccountException.notFound("account not found"));
    }

    @Transactional
    public AccountResponse setEnabled(String id, boolean enabled, String actor) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> AccountException.notFound("account not found"));
        account.setEnabled(enabled);
        Account saved = accountRepository.save(account);
        events.emit(IdentityEvent.Types.USER_STATUS, actor, id + " enabled=" + enabled,
                java.util.Map.of("userId", id, "enabled", enabled));
        return AccountResponse.from(saved);
    }

    @Transactional
    public AccountResponse setRoles(String id, String rolesCsv, String actor) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> AccountException.notFound("account not found"));
        if (!StringUtils.hasText(rolesCsv)) {
            throw AccountException.badRequest("roles required");
        }
        account.setRoles(rolesCsv.trim().toUpperCase(Locale.ROOT));
        Account saved = accountRepository.save(account);
        events.emit(IdentityEvent.Types.USER_ROLES, actor, id + " -> " + saved.getRoles(),
                java.util.Map.of("userId", id, "roles", saved.getRoles()));
        return AccountResponse.from(saved);
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        Account account = requireEntityByUsername(username);
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw AccountException.badRequest("current password incorrect");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw AccountException.badRequest("new password too short");
        }
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
        events.emit(IdentityEvent.Types.PASSWORD_CHANGED, username, java.util.Map.of());
    }

    @Transactional(readOnly = true)
    public Account authenticatePassword(String username, String password) {
        Account account = accountRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> AccountException.badRequest("invalid credentials"));
        if (!account.isEnabled() || !passwordEncoder.matches(password, account.getPasswordHash())) {
            throw AccountException.badRequest("invalid credentials");
        }
        return account;
    }

    @Transactional
    public TotpSetupResponse beginTotpSetup(String username) {
        Account account = requireEntityByUsername(username);
        String secret = totpService.generateSecret();
        account.setTotpSecret(secret);
        account.setTotpEnabled(false);
        accountRepository.save(account);
        String url = totpService.otpAuthUrl("AAAX", account.getUsername(), secret);
        return new TotpSetupResponse(secret, url);
    }

    @Transactional
    public AccountResponse confirmTotp(String username, String code) {
        Account account = requireEntityByUsername(username);
        if (!StringUtils.hasText(account.getTotpSecret())) {
            throw AccountException.badRequest("totp setup not started");
        }
        if (!totpService.verify(account.getTotpSecret(), code)) {
            throw AccountException.badRequest("invalid totp code");
        }
        account.setTotpEnabled(true);
        Account saved = accountRepository.save(account);
        events.emit(IdentityEvent.Types.MFA_TOTP_ENABLED, username, java.util.Map.of());
        return AccountResponse.from(saved);
    }

    @Transactional
    public AccountResponse disableTotp(String username, String password, String code) {
        Account account = requireEntityByUsername(username);
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw AccountException.badRequest("password incorrect");
        }
        if (account.isTotpEnabled() && !totpService.verify(account.getTotpSecret(), code)) {
            throw AccountException.badRequest("invalid totp code");
        }
        account.setTotpEnabled(false);
        account.setTotpSecret(null);
        Account saved = accountRepository.save(account);
        events.emit(IdentityEvent.Types.MFA_TOTP_DISABLED, username, java.util.Map.of());
        return AccountResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public boolean verifyTotp(String username, String code) {
        Account account = requireEntityByUsername(username);
        if (!account.isTotpEnabled()) {
            return true;
        }
        return totpService.verify(account.getTotpSecret(), code);
    }

    @Transactional
    public Account linkOrCreateGoogle(String sub, String email, String nameHint) {
        return accountRepository.findByGoogleSub(sub)
                .or(() -> email != null ? accountRepository.findByEmailIgnoreCase(email) : java.util.Optional.empty())
                .map(existing -> {
                    if (existing.getGoogleSub() == null) {
                        existing.setGoogleSub(sub);
                        return accountRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> createFederatedUser(email, nameHint, a -> a.setGoogleSub(sub), "account.google.link", sub));
    }

    @Transactional
    public Account linkOrCreateSaml(String nameId, String email, String nameHint) {
        return accountRepository.findBySamlNameId(nameId)
                .or(() -> email != null ? accountRepository.findByEmailIgnoreCase(email) : java.util.Optional.empty())
                .map(existing -> {
                    if (existing.getSamlNameId() == null) {
                        existing.setSamlNameId(nameId);
                        return accountRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> createFederatedUser(email, nameHint, a -> a.setSamlNameId(nameId), "account.saml.link", nameId));
    }

    private Account createFederatedUser(
            String email,
            String nameHint,
            java.util.function.Consumer<Account> linker,
            String auditAction,
            String auditDetail) {
        String base = StringUtils.hasText(nameHint) ? nameHint.replaceAll("[^a-zA-Z0-9._-]", "") : "user";
        if (base.length() < 3) {
            base = "user";
        }
        String username = base.substring(0, Math.min(base.length(), 40));
        int i = 0;
        while (accountRepository.existsByUsernameIgnoreCase(username)) {
            i++;
            username = base.substring(0, Math.min(base.length(), 36)) + i;
        }
        String randomPass = passwordEncoder.encode(UUID.randomUUID().toString());
        Account created = new Account(username, normalizeEmail(email), randomPass, "USER");
        linker.accept(created);
        Account saved = accountRepository.save(created);
        events.emit(auditAction, username, auditDetail, java.util.Map.of("federation", auditDetail == null ? "" : auditDetail));
        return saved;
    }

    @Transactional(readOnly = true)
    public void requestPasswordReset(String usernameOrEmail) {
        if (!StringUtils.hasText(usernameOrEmail)) {
            return;
        }
        String q = usernameOrEmail.trim();
        Account account = accountRepository.findByUsernameIgnoreCase(q)
                .or(() -> accountRepository.findByEmailIgnoreCase(q.toLowerCase(Locale.ROOT)))
                .orElse(null);
        if (account == null || !account.isEnabled()) {
            return;
        }
        String code = generateOtp();
        otpStore.put(resetKey(account.getUsername()), code, java.time.Instant.now().plusSeconds(otpTtlSeconds));
        String destination = account.getEmail() != null ? account.getEmail() : account.getUsername();
        otpSender.send(destination, code);
    }

    @Transactional
    public void resetPassword(String username, String code, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw AccountException.badRequest("new password too short");
        }
        Account account = requireEntityByUsername(username);
        InMemoryOtpStore.Entry entry = otpStore.get(resetKey(account.getUsername()));
        if (entry == null || !entry.code().equals(code == null ? "" : code.trim())) {
            throw AccountException.badRequest("invalid or expired otp");
        }
        otpStore.remove(resetKey(account.getUsername()));
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
        events.emit(IdentityEvent.Types.PASSWORD_RESET, username, java.util.Map.of());
    }

    public long countUsers() {
        return accountRepository.count();
    }

    public long countAdmins() {
        return accountRepository.countByRolesContainingIgnoreCase("ADMIN");
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, otpLength);
        int n = java.util.concurrent.ThreadLocalRandom.current().nextInt(bound / 10, bound);
        return String.valueOf(n);
    }

    static String resetKey(String username) {
        return "reset:" + username.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 128) String newPassword
    ) {
    }

    public record ForgotPasswordRequest(
            @NotBlank @Size(max = 320) String usernameOrEmail
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(min = 4, max = 10) String code,
            @NotBlank @Size(min = 8, max = 128) String newPassword
    ) {
    }

    public record SetEnabledRequest(boolean enabled) {
    }

    public record SetRolesRequest(@NotBlank String roles) {
    }

    public record TotpSetupResponse(String secret, String otpauthUrl) {
    }

    public record TotpCodeRequest(@NotBlank @Size(min = 6, max = 6) String code) {
    }

    public record DisableTotpRequest(
            @NotBlank String password,
            @NotBlank @Size(min = 6, max = 6) String code
    ) {
    }

    public record BootstrapAdminRequest(
            @NotBlank @Size(max = 64) String username,
            @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            String bootstrapToken
    ) {
    }
}
