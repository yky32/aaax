package com.aaax.usecase.account;

import java.util.List;

import com.aaax.entity.dto.response.GetAccountResponseDto;
import com.aaax.entity.po.account.Account;
import com.aaax.exception.AccountException;
import com.aaax.repository.AccountRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Read-side account queries (not write workflows). */
@Component
public class AccountQueries {

    private final AccountRepository accountRepository;
    private final FederateAccountUseCase federateAccountUseCase;

    public AccountQueries(AccountRepository accountRepository, FederateAccountUseCase federateAccountUseCase) {
        this.accountRepository = accountRepository;
        this.federateAccountUseCase = federateAccountUseCase;
    }

    @Transactional(readOnly = true)
    public GetAccountResponseDto requireByUsername(String username) {
        return toDto(requireEntityByUsername(username));
    }

    @Transactional(readOnly = true)
    public Account requireEntityByUsername(String username) {
        return accountRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(() -> AccountException.notFound("account not found"));
    }

    @Transactional(readOnly = true)
    public List<GetAccountResponseDto> listAll() {
        return accountRepository.findAllByOrderByCreateDtDesc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public GetAccountResponseDto getById(String id) {
        return accountRepository
                .findById(id)
                .map(this::toDto)
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

    private GetAccountResponseDto toDto(Account account) {
        return GetAccountResponseDto.from(account, federateAccountUseCase.linkedProvidersList(account));
    }
}
