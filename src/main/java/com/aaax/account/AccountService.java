package com.aaax.account;

import java.util.List;

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
    private final int otpTtlSeconds;
    private final int otpLength;

    public AccountService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            InMemoryOtpStore otpStore,
            OtpSender otpSender,
            @Value("${aaax.otp.ttl-seconds:300}") int otpTtlSeconds,
            @Value("${aaax.otp.length:6}") int otpLength) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpStore = otpStore;
        this.otpSender = otpSender;
        this.otpTtlSeconds = otpTtlSeconds;
        this.otpLength = Math.max(4, Math.min(otpLength, 10));
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
        return AccountResponse.from(accountRepository.save(account));
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
        return accountRepository.findAllByOrderByUsernameAsc().stream()
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
    public AccountResponse setEnabled(String id, boolean enabled) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> AccountException.notFound("account not found"));
        account.setEnabled(enabled);
        return AccountResponse.from(accountRepository.save(account));
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
    }

    /**
     * Always returns 202-shaped success to avoid account enumeration.
     */
    @Transactional(readOnly = true)
    public void requestPasswordReset(String usernameOrEmail) {
        if (!StringUtils.hasText(usernameOrEmail)) {
            return;
        }
        String q = usernameOrEmail.trim();
        Account account = accountRepository.findByUsernameIgnoreCase(q)
                .or(() -> accountRepository.findByEmailIgnoreCase(q.toLowerCase()))
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
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, otpLength);
        int n = java.util.concurrent.ThreadLocalRandom.current().nextInt(bound / 10, bound);
        return String.valueOf(n);
    }

    static String resetKey(String username) {
        return "reset:" + username.trim().toLowerCase();
    }

    private static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase();
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
}
