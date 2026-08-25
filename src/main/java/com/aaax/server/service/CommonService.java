package com.aaax.server.service;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;
import com.aaax.core.utils.JSONUtil;
import com.aaax.server.entity.dto.response.GetSystemConfigurationRequestDto;
import com.aaax.server.usecase.SystemConfigurationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommonService {

    private final ResourceLoader resourceLoader;
    private final SystemConfigurationUseCase systemConfigurationUseCase;

    public boolean isValidSourceSystem(String ss) {
        GetSystemConfigurationRequestDto config = systemConfigurationUseCase.query("AVAILABLE_SOURCE_SYSTEM_OPTIONS", "GLOBAL");
        List<String> sourceSystems = JSONUtil.convertListFromObject(config.getValue(), String[].class);
        boolean isValid = sourceSystems.contains(ss);
        if (!isValid) {
            throw new BizException(SystemResponse.PAM0400, Map.of("availableSourceSystems", sourceSystems, "input", ss));
        }
        return true;
    }
}
