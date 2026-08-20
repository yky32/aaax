package com.aaax.account;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a local demo account when the store is empty.
 */
@Component
public class DemoAccountSeed implements ApplicationRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoAccountSeed(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (accountRepository.count() > 0) {
            return;
        }
        Account demo = new Account("demo", "demo@aaax.local", passwordEncoder.encode("demo"));
        accountRepository.save(demo);
    }
}
