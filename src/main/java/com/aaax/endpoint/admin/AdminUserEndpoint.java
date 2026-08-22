package com.aaax.endpoint.admin;

import java.security.Principal;
import java.util.List;

import com.aaax.entity.dto.response.GetAccountResponseDto;
import com.aaax.entity.dto.request.SetAccountEnabledRequestDto;
import com.aaax.entity.dto.request.SetAccountRolesRequestDto;
import com.aaax.usecase.account.AccountQueries;
import com.aaax.usecase.account.AdminManageUserUseCase;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/users")
public class AdminUserEndpoint {

    private final AccountQueries queries;
    private final AdminManageUserUseCase manageUser;

    public AdminUserEndpoint(AccountQueries queries, AdminManageUserUseCase manageUser) {
        this.queries = queries;
        this.manageUser = manageUser;
    }

    @GetMapping
    public List<GetAccountResponseDto> list() {
        return queries.listAll();
    }

    @GetMapping("/{id}")
    public GetAccountResponseDto get(@PathVariable String id) {
        return queries.getById(id);
    }

    @PatchMapping("/{id}/status")
    public GetAccountResponseDto setStatus(
            @PathVariable String id, @Valid @RequestBody SetAccountEnabledRequestDto request, Principal principal) {
        return manageUser.setEnabled(id, request.enabled(), principal.getName());
    }

    @PatchMapping("/{id}/roles")
    public GetAccountResponseDto setRoles(
            @PathVariable String id, @Valid @RequestBody SetAccountRolesRequestDto request, Principal principal) {
        return manageUser.setRoles(id, request.roles(), principal.getName());
    }
}
