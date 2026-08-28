package com.aaax.server.usecase;

import com.aaax.core.exception.BizException;
import com.aaax.server.entity.dto.request.CreateRegisteredClientRequestDto;
import com.aaax.server.entity.dto.response.ClientResponseDto;
import com.aaax.server.exception.response.ClientErrorResponse;
import com.aaax.server.exception.response.UaaSettingErrorResponse;
import com.aaax.server.utils.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UaaSettingUseCase {
    @Value("${aaax.security.server.expiry-time}")
    private Integer serverTokenExpiryTime;
    @Value("${aaax.security.registered-client.secret}")
    private String registeredClientSecret;
    private final PasswordEncoder passwordEncoder;
    private final RegisteredClientRepository registeredClientRepository;

    public RegisteredClient createRegisteredClient(CreateRegisteredClientRequestDto dto) {
        // REMINDER: for generating the insert SQL of registered client via programmatically
        RegisteredClient _registeredClient = registeredClientRepository.findByClientId(dto.getClientId());
        if (_registeredClient != null) {
            return _registeredClient;
        }

        RegisteredClient registeredClient = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId(dto.getClientId())
                .clientSecret(passwordEncoder.encode(registeredClientSecret)) // # https://www.base64encode.org/ [client:secret]
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofSeconds(dto.getTokenExpiryTime()))     //TODO
                        .build()
                )
                .build();
        registeredClientRepository.save(registeredClient);
        return registeredClient;
    }

    /**
     * Create client ID
     *
     * @param dto
     * @return
     */
    public ClientResponseDto create(CreateRegisteredClientRequestDto dto) {
        // REMINDER: for generating the insert SQL of registered client via programmatically
        RegisteredClient _registeredClient = registeredClientRepository.findByClientId(dto.getClientId());
        if (_registeredClient != null) {
            return search(_registeredClient.getId());
        }

        // generate client secret
        String clientSecret = PasswordUtil.generateCommonLangPassword();
        RegisteredClient registeredClient = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId(dto.getClientId())
                .clientName(Optional.ofNullable(dto.getClientName()).orElse(UUID.randomUUID().toString()))
                .clientSecret(passwordEncoder.encode(clientSecret)) // # https://www.base64encode.org/ [client:secret]
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofSeconds(dto.getTokenExpiryTime()))     //TODO
                        .build()
                )
                .build();
        registeredClientRepository.save(registeredClient);

        return ClientResponseDto.builder()
                .id(registeredClient.getId())
                .clientId(registeredClient.getClientId())
                .clientSecret(clientSecret)
                .build();
    }

    /**
     * Get client ID
     *
     * @param id
     * @return
     */
    public ClientResponseDto search(String id) {
        // find registered client by uuid
        RegisteredClient registeredClient = registeredClientRepository.findById(id);
        return ClientResponseDto.builder()
                .id(registeredClient.getId())
                .clientId(registeredClient.getClientId())
                .build();
    }

    /**
     * Update client secret
     *
     * @param id
     * @return
     */
    public ClientResponseDto updateSecret(String id) {

        // find registered client by uuid
        RegisteredClient registeredClient = registeredClientRepository.findById(id);

        if (registeredClient == null) {
            // invalid client id
            throw new BizException(ClientErrorResponse.CLT0001, id);
        }

        // new client secret
        String clientSecret = PasswordUtil.generateCommonLangPassword();

        // update client secret
        RegisteredClient newRegisteredClient = RegisteredClient.from(registeredClient)
                .clientSecret(passwordEncoder.encode(clientSecret))
                .build();
        registeredClientRepository.save(newRegisteredClient);

        return ClientResponseDto.builder()
                .id(registeredClient.getId())
                .clientId(registeredClient.getClientId())
                .clientSecret(clientSecret).build();
    }

    public String getBasicAuthorization(String id) {
        RegisteredClient _registeredClient = registeredClientRepository.findById(id);
        if (_registeredClient == null) {
            throw new BizException(UaaSettingErrorResponse.UAS0001, "id =>".concat(id));
        }
        // Encode the string to Base64
        String clientString = _registeredClient.getClientId().concat(":").concat(registeredClientSecret);
        return Base64.getEncoder().encodeToString(clientString.getBytes());
    }
}
