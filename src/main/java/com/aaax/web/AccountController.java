package com.aaax.web;

import java.security.Principal;

import com.aaax.account.AccountResponse;
import com.aaax.account.AccountService;
import com.aaax.account.RegisterAccountRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse register(@Valid @RequestBody RegisterAccountRequest request) {
        return accountService.register(request);
    }

    @GetMapping("/me")
    public AccountResponse me(Principal principal) {
        return accountService.requireByUsername(principal.getName());
    }
}
