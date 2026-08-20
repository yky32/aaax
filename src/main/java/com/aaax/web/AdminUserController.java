package com.aaax.web;

import java.security.Principal;
import java.util.List;

import com.aaax.account.AccountResponse;
import com.aaax.account.application.AccountDtos.SetEnabledRequest;
import com.aaax.account.application.AccountDtos.SetRolesRequest;
import com.aaax.account.application.AccountQueries;
import com.aaax.account.application.AdminManageUserUseCase;

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

    private final AccountQueries queries;
    private final AdminManageUserUseCase manageUser;

    public AdminUserController(AccountQueries queries, AdminManageUserUseCase manageUser) {
        this.queries = queries;
        this.manageUser = manageUser;
    }

    @GetMapping
    public List<AccountResponse> list() {
        return queries.listAll();
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable String id) {
        return queries.getById(id);
    }

    @PatchMapping("/{id}/status")
    public AccountResponse setStatus(
            @PathVariable String id, @Valid @RequestBody SetEnabledRequest request, Principal principal) {
        return manageUser.setEnabled(id, request.enabled(), principal.getName());
    }

    @PatchMapping("/{id}/roles")
    public AccountResponse setRoles(
            @PathVariable String id, @Valid @RequestBody SetRolesRequest request, Principal principal) {
        return manageUser.setRoles(id, request.roles(), principal.getName());
    }
}
