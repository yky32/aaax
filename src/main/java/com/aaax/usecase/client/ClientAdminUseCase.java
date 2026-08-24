package com.aaax.usecase.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.aaax.core.exception.BizException;
import com.aaax.exception.response.ClientErrorResponse;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ClientAdminUseCase {

    private final RegisteredClientRepository registeredClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final IdentityEventBus identityEventBus;

    public ClientAdminUseCase(
            RegisteredClientRepository registeredClientRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate,
            IdentityEventBus identityEventBus) {
        this.registeredClientRepository = registeredClientRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.identityEventBus = identityEventBus;
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> list() {
        return jdbcTemplate.query(
                "SELECT client_id FROM oauth2_registered_client ORDER BY client_id",
                (rs, i) -> {
                    RegisteredClient c = registeredClientRepository.findByClientId(rs.getString("client_id"));
                    return c == null ? null : ClientResponse.from(c);
                }).stream().filter(c -> c != null).toList();
    }

    @Transactional(readOnly = true)
    public ClientResponse get(String clientId) {
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client == null) {
            throw new BizException(ClientErrorResponse.CLT0002, "client not found");
        }
        return ClientResponse.from(client);
    }

    @Transactional
    public ClientCreatedResponse create(@Valid CreateClientRequest request) {
        if (registeredClientRepository.findByClientId(request.clientId()) != null) {
            throw new BizException(ClientErrorResponse.CLT0409, "client_id already exists");
        }
        String rawSecret = request.clientSecret() != null && !request.clientSecret().isBlank()
                ? request.clientSecret()
                : UUID.randomUUID().toString().replace("-", "");

        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(request.clientId().trim())
                .clientSecret(passwordEncoder.encode(rawSecret))
                .clientName(request.clientName() == null || request.clientName().isBlank()
                        ? request.clientId().trim()
                        : request.clientName().trim())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);

        grantTypes(request).forEach(builder::authorizationGrantType);
        if (request.redirectUris() != null) {
            request.redirectUris().forEach(builder::redirectUri);
        }
        if (request.postLogoutRedirectUris() != null) {
            request.postLogoutRedirectUris().forEach(builder::postLogoutRedirectUri);
        }
        scopes(request).forEach(builder::scope);

        builder.clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(request.consentRequired())
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(
                                request.accessTokenHours() == null ? 1 : request.accessTokenHours()))
                        .refreshTokenTimeToLive(Duration.ofDays(
                                request.refreshTokenDays() == null ? 30 : request.refreshTokenDays()))
                        .build());

        registeredClientRepository.save(builder.build());
        identityEventBus.emit(IdentityEvent.Types.CLIENT_CREATED, request.clientId().trim(),
                java.util.Map.of("clientId", request.clientId().trim()));
        return new ClientCreatedResponse(ClientResponse.from(registeredClientRepository.findByClientId(request.clientId())), rawSecret);
    }

    @Transactional
    public void delete(String clientId) {
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client == null) {
            throw new BizException(ClientErrorResponse.CLT0002, "client not found");
        }
        jdbcTemplate.update("DELETE FROM oauth2_authorization_consent WHERE registered_client_id = ?", client.getId());
        jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE registered_client_id = ?", client.getId());
        jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE id = ?", client.getId());
        identityEventBus.emit(IdentityEvent.Types.CLIENT_DELETED, clientId, java.util.Map.of("clientId", clientId));
    }

    private static Set<AuthorizationGrantType> grantTypes(CreateClientRequest request) {
        Set<AuthorizationGrantType> grants = new HashSet<>();
        List<String> raw = request.grantTypes() == null || request.grantTypes().isEmpty()
                ? List.of("authorization_code", "refresh_token", "client_credentials")
                : request.grantTypes();
        for (String g : raw) {
            grants.add(new AuthorizationGrantType(g.trim()));
        }
        return grants;
    }

    private static Set<String> scopes(CreateClientRequest request) {
        if (request.scopes() == null || request.scopes().isEmpty()) {
            return Set.of("openid", "profile", "api.read");
        }
        return new HashSet<>(request.scopes());
    }

    public record CreateClientRequest(
            @NotBlank @Size(max = 100) String clientId,
            @Size(max = 200) String clientName,
            @Size(max = 200) String clientSecret,
            List<@NotBlank String> redirectUris,
            List<String> postLogoutRedirectUris,
            List<String> grantTypes,
            List<String> scopes,
            Boolean requireConsent,
            Integer accessTokenHours,
            Integer refreshTokenDays
    ) {
        public boolean consentRequired() {
            return requireConsent != null && requireConsent;
        }
    }

    public record ClientResponse(
            String id,
            String clientId,
            String clientName,
            List<String> redirectUris,
            List<String> grantTypes,
            List<String> scopes
    ) {
        static ClientResponse from(RegisteredClient c) {
            return new ClientResponse(
                    c.getId(),
                    c.getClientId(),
                    c.getClientName(),
                    new ArrayList<>(c.getRedirectUris()),
                    c.getAuthorizationGrantTypes().stream().map(AuthorizationGrantType::getValue).sorted().toList(),
                    c.getScopes().stream().sorted().toList());
        }
    }

    public record ClientCreatedResponse(ClientResponse client, String clientSecret) {
    }
}
