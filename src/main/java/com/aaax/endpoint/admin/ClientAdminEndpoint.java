package com.aaax.endpoint.admin;

import java.util.List;

import com.aaax.usecase.client.ClientAdminUseCase;
import com.aaax.usecase.client.ClientAdminUseCase.ClientCreatedResponse;
import com.aaax.usecase.client.ClientAdminUseCase.ClientResponse;
import com.aaax.usecase.client.ClientAdminUseCase.CreateClientRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/clients")
public class ClientAdminEndpoint {

    private final ClientAdminUseCase clientAdminService;

    public ClientAdminEndpoint(ClientAdminUseCase clientAdminService) {
        this.clientAdminService = clientAdminService;
    }

    @GetMapping
    public List<ClientResponse> list() {
        return clientAdminService.list();
    }

    @GetMapping("/{clientId}")
    public ClientResponse get(@PathVariable String clientId) {
        return clientAdminService.get(clientId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientCreatedResponse create(@Valid @RequestBody CreateClientRequest request) {
        return clientAdminService.create(request);
    }

    @DeleteMapping("/{clientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String clientId) {
        clientAdminService.delete(clientId);
    }
}
