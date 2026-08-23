package com.aaax.usecase.account;

import java.util.List;

import com.aaax.entity.po.Account;
import com.aaax.exception.AccountException;
import com.aaax.repository.AccountRepository;
import com.aaax.entity.dto.response.GetAccountResponseDto;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Read-side account queries (not write workflows). */
@Component
public class AccountQueries {

    private final AccountRepository accountRepository;

    public AccountQueries(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public GetAccountResponseDto requireByUsername(String username) {
        return GetAccountResponseDto.from(requireEntityByUsername(username));
    }

    @Transactional(readOnly = true)
    public Account requireEntityByUsername(String username) {
        return accountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> AccountException.notFound("account not found"));
    }

    @Transactional(readOnly = true)
    public List<GetAccountResponseDto> listAll() {
        return accountRepository.findAllByOrderByCreateDtDesc().stream().map(GetAccountResponseDto::from).toList();
    }

    @Transactional(readOnly = true)
    public GetAccountResponseDto getById(String id) {
        return accountRepository.findById(id)
                .map(GetAccountResponseDto::from)
                .orElseThrow(() -> AccountException.notFound("account not found"));
    }

    @Transactional(readOnly = true)
    public boolean needsBootstrap() {
        return accountRepository.countByRolesContainingIgnoreCase("ADMIN") == 0;
    }

    public long countUsers() {
        return accountRepository.count();
    }

    public long countAdmins() {
        return accountRepository.countByRolesContainingIgnoreCase("ADMIN");
    }
}
