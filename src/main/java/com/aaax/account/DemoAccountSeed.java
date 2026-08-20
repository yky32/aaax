package com.aaax.account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds local demo accounts when missing.
 */
@Component
@Order(10)
public class DemoAccountSeed implements ApplicationRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public DemoAccountSeed(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${aaax.demo.seed-account:true}") boolean enabled) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (!accountRepository.existsByUsernameIgnoreCase("demo")) {
            accountRepository.save(new Account(
                    "demo", "demo@aaax.local", passwordEncoder.encode("demo"), "USER"));
        }
        if (!accountRepository.existsByUsernameIgnoreCase("admin")) {
            accountRepository.save(new Account(
                    "admin", "admin@aaax.local", passwordEncoder.encode("admin12345"), "USER,ADMIN"));
        }
    }
}
