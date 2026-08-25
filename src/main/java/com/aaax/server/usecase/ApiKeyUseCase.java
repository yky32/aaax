package com.aaax.server.usecase;

import com.aaax.server.entity.dto.request.CreateApiKeyRequestDto;
import com.aaax.server.entity.dto.response.ClientResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyUseCase {

    public ClientResponseDto create(CreateApiKeyRequestDto dto) {
        return null;
    }
}
