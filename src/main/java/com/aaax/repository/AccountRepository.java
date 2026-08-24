package com.aaax.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.aaax.entity.po.account.Account;

public interface AccountRepository extends JpaRepository<Account, String> {

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Account> findByUsernameIgnoreCase(String username);

    Optional<Account> findByEmailIgnoreCase(String email);

    Optional<Account> findBySamlNameId(String samlNameId);

    boolean existsByRolesContainingIgnoreCase(String roleFragment);

    long countByRolesContainingIgnoreCase(String roleFragment);

    List<Account> findAllByOrderByCreateDtDesc();
}
