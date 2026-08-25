package com.aaax.service;

import com.aaax.core.exception.BizException;
import com.aaax.entity.dto.response.GetSystemConfigurationRequestDto;
import com.aaax.usecase.SystemConfigurationUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonServiceTest {

    @Mock
    private ResourceLoader resourceLoader;
    @Mock
    private SystemConfigurationUseCase systemConfigurationUseCase;

    @InjectMocks
    private CommonService commonService;

    @Test
    @DisplayName("isValidSourceSystem should return true for allowed system")
    void isValidSourceSystem_shouldReturnTrue() {
        when(systemConfigurationUseCase.query("AVAILABLE_SOURCE_SYSTEM_OPTIONS", "GLOBAL"))
                .thenReturn(GetSystemConfigurationRequestDto.builder()
                        .value(List.of("app", "admin"))
                        .build());

        assertTrue(commonService.isValidSourceSystem("app"));
    }

    @Test
    @DisplayName("isValidSourceSystem should throw for unknown system")
    void isValidSourceSystem_shouldThrowForUnknown() {
        when(systemConfigurationUseCase.query("AVAILABLE_SOURCE_SYSTEM_OPTIONS", "GLOBAL"))
                .thenReturn(GetSystemConfigurationRequestDto.builder()
                        .value(List.of("app"))
                        .build());

        assertThrows(BizException.class, () -> commonService.isValidSourceSystem("unknown"));
    }
}
