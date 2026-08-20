package com.aaax.web;

import java.util.List;

import com.aaax.account.AccountResponse;
import com.aaax.account.AccountService;
import com.aaax.account.AccountService.SetEnabledRequest;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/users")
public class AdminUserController {

    private final AccountService accountService;

    public AdminUserController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<AccountResponse> list() {
        return accountService.listAll();
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable String id) {
        return accountService.getById(id);
    }

    @PatchMapping("/{id}/status")
    public AccountResponse setStatus(@PathVariable String id, @Valid @RequestBody SetEnabledRequest request) {
        return accountService.setEnabled(id, request.enabled());
    }
}
