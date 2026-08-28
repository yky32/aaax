package com.aaax.server.endpoint.aaax_config;

import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.server.usecase.AaaxSettingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@Slf4j
public class AaaxSettingEndpoint {

    private final AaaxSettingUseCase aaaxSettingUseCase;

    @GetMapping("/registered-clients/{id}/basic-authorization")
    public Result<String> get(@PathVariable String id) {
        return R.success(aaaxSettingUseCase.getBasicAuthorization(id));
    }
}
