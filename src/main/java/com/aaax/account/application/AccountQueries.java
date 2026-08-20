package com.aaax.account.application;

import java.util.List;

import com.aaax.account.Account;
import com.aaax.account.AccountException;
import com.aaax.account.AccountRepository;
import com.aaax.account.AccountResponse;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Read-side account queries (not write workflows). */
@Component
public class AccountQueries {

    private final AccountRepository accounts;

    public AccountQueries(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public AccountResponse requireByUsername(String username) {
        return AccountResponse.from(requireEntityByUsername(username));
    }

    @Transactional(readOnly = true)
    public Account requireEntityByUsername(String username) {
        return accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> AccountException.notFound("account not found"));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAll() {
        return accounts.findAllByOrderByCreatedAtDesc().stream().map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getById(String id) {
        return accounts.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> AccountException.notFound("account not found"));
    }

    @Transactional(readOnly = true)
    public boolean needsBootstrap() {
        return accounts.countByRolesContainingIgnoreCase("ADMIN") == 0;
    }

    public long countUsers() {
        return accounts.count();
    }

    public long countAdmins() {
        return accounts.countByRolesContainingIgnoreCase("ADMIN");
    }
}
