package com.aaax.server.endpoint.api;

import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.server.entity.dto.request.CreateApiKeyRequestDto;
import com.aaax.server.entity.dto.response.ClientResponseDto;
import com.aaax.server.usecase.ApiKeyUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Api Key Management
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-keys")
@Slf4j
public class ApiKeyEndpoint {

    private final ApiKeyUseCase apiKeyUseCase;

    @PostMapping
    public Result<ClientResponseDto> create(@RequestBody CreateApiKeyRequestDto dto) {
        return R.success(apiKeyUseCase.create(dto));
    }

}
