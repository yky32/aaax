package com.aaax.usecase;

import com.aaax.core.exception.BizException;
import com.aaax.core.utils.RedisUtil;
import com.aaax.entity.dto.request.CreateSystemConfigurationRequestDto;
import com.aaax.entity.dto.response.GetSystemConfigurationRequestDto;
import com.aaax.entity.po.configuration.SystemConfiguration;
import com.aaax.repository.SystemConfigurationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemConfigurationUseCaseTest {

    @Mock
    private SystemConfigurationRepository systemConfigurationRepository;
    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private SystemConfigurationUseCase systemConfigurationUseCase;

    @Test
    @DisplayName("create should persist and wrap configuration")
    void create_shouldPersist() {
        when(systemConfigurationRepository.existsByTargetAndScope("OTP", "GLOBAL")).thenReturn(false);
        when(systemConfigurationRepository.save(any())).thenAnswer(inv -> {
            SystemConfiguration sc = inv.getArgument(0);
            sc.setId(10L);
            return sc;
        });

        GetSystemConfigurationRequestDto result = systemConfigurationUseCase.create(
                CreateSystemConfigurationRequestDto.builder()
                        .name("OTP")
                        .target("otp")
                        .value(300)
                        .build());

        assertEquals("sc_10", result.getId());
        assertEquals("OTP", result.getTarget());
        assertEquals("GLOBAL", result.getScope());
    }

    @Test
    @DisplayName("query should return matching scope then GLOBAL fallback")
    void query_shouldPreferExactScope() {
        SystemConfiguration scoped = SystemConfiguration.builder()
                .id(1L).target("OTP").scope("APP").value(100).build();
        SystemConfiguration global = SystemConfiguration.builder()
                .id(2L).target("OTP").scope("GLOBAL").value(200).build();
        when(systemConfigurationRepository.findAllByTarget("OTP"))
                .thenReturn(List.of(scoped, global));

        assertEquals(100, systemConfigurationUseCase.query("otp", "APP").getValue());
        assertEquals(200, systemConfigurationUseCase.query("otp", "OTHER").getValue());
    }

    @Test
    @DisplayName("query should throw when target missing")
    void query_shouldThrowWhenMissing() {
        when(systemConfigurationRepository.findAllByTarget("MISSING")).thenReturn(List.of());
        assertThrows(BizException.class, () -> systemConfigurationUseCase.query("missing", "GLOBAL"));
    }

    @Test
    @DisplayName("update should mutate fields and persist")
    void update_shouldMutate() {
        SystemConfiguration existing = SystemConfiguration.builder()
                .id(5L).name("old").target("OLD").scope("GLOBAL").value(1).build();
        when(systemConfigurationRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(systemConfigurationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GetSystemConfigurationRequestDto result = systemConfigurationUseCase.update("sc_5",
                CreateSystemConfigurationRequestDto.builder()
                        .name("new").target("NEW").scope("APP").value(2).build());

        assertEquals("new", result.getName());
        assertEquals("NEW", existing.getTarget());
        assertEquals("APP", existing.getScope());
        assertEquals(2, existing.getValue());
        verify(systemConfigurationRepository).save(existing);
        verify(redisUtil, atLeastOnce()).delete(anyString());
    }

    @Test
    @DisplayName("list should return mapped rows")
    void list_shouldReturnRows() {
        SystemConfiguration row = SystemConfiguration.builder()
                .id(1L).name("OTP").target("OTP").scope("GLOBAL").value(60).build();
        row.setIsActive(true);
        when(systemConfigurationRepository.findAll()).thenReturn(List.of(row));

        List<GetSystemConfigurationRequestDto> result = systemConfigurationUseCase.list(null);

        assertEquals(1, result.size());
        assertEquals("sc_1", result.get(0).getId());
        assertEquals("OTP", result.get(0).getTarget());
    }

    @Test
    @DisplayName("setActive should flip isActive and save")
    void setActive_shouldPersist() {
        SystemConfiguration existing = SystemConfiguration.builder()
                .id(7L).name("x").target("X").scope("GLOBAL").value(1).build();
        existing.setIsActive(true);
        when(systemConfigurationRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(systemConfigurationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GetSystemConfigurationRequestDto result = systemConfigurationUseCase.setActive("sc_7", false);

        assertEquals(false, existing.getIsActive());
        assertEquals(false, result.getIsActive());
        verify(systemConfigurationRepository).save(existing);
    }

    @Test
    @DisplayName("myConfigurations should use redis cache when present")
    void myConfigurations_shouldUseCache() {
        GetSystemConfigurationRequestDto cached = GetSystemConfigurationRequestDto.builder()
                .id("sc_1").target("OTP").scope("GLOBAL").value(60).build();
        when(redisUtil.getOrLoad(anyString(), eq(GetSystemConfigurationRequestDto.class), any()))
                .thenReturn(cached);

        GetSystemConfigurationRequestDto result = systemConfigurationUseCase.myConfigurations("OTP");

        assertEquals("sc_1", result.getId());
        verify(systemConfigurationRepository, never()).findByTargetAndScope(any(), any());
    }

    @Test
    @DisplayName("myConfigurations should load DB and cache on miss")
    void myConfigurations_shouldLoadDbOnMiss() {
        when(redisUtil.getOrLoad(anyString(), eq(GetSystemConfigurationRequestDto.class), any()))
                .thenAnswer(invocation -> {
                    java.util.function.Supplier<?> loader = invocation.getArgument(2);
                    return loader.get();
                });
        SystemConfiguration config = SystemConfiguration.builder()
                .id(3L).name("OTP").target("OTP").scope("GLOBAL").value(90).build();
        when(systemConfigurationRepository.findByTargetAndScope("OTP", "GLOBAL"))
                .thenReturn(Optional.of(config));

        GetSystemConfigurationRequestDto result = systemConfigurationUseCase.myConfigurations("OTP", "GLOBAL");

        assertEquals("sc_3", result.getId());
        verify(redisUtil).set(anyString(), eq(config), eq(300L));
    }

    @Test
    @DisplayName("getOptionalSystemConfig should fallback to GLOBAL")
    void getOptionalSystemConfig_shouldFallback() {
        when(systemConfigurationRepository.findByTargetAndScope("OTP", "APP")).thenReturn(Optional.empty());
        SystemConfiguration global = SystemConfiguration.builder().id(1L).build();
        when(systemConfigurationRepository.findByTargetAndScope("OTP", "GLOBAL")).thenReturn(Optional.of(global));

        assertTrue(systemConfigurationUseCase.getOptionalSystemConfig("OTP", "APP").isPresent());
    }
}
