package com.aaax.endpoint.uaa_config;

import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.entity.dto.request.CreateRegisteredClientRequestDto;
import com.aaax.usecase.UaaSettingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Client Management
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/clients")
@Slf4j
public class ClientEndpoint {


    private final UaaSettingUseCase uaaSettingUseCase;


    /**
     * Create client ID
     *
     * @param clientRequestDto
     * @return
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Result create(@RequestBody CreateRegisteredClientRequestDto clientRequestDto) {
        return R.success(uaaSettingUseCase.create(clientRequestDto));
    }

    /**
     * Get client ID
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result search(@PathVariable String id) {
        return R.success(uaaSettingUseCase.search(id));
    }

    /**
     * Update client secret
     *
     * @param id
     * @return
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result updateSecret(@PathVariable String id) {
        return R.success(uaaSettingUseCase.updateSecret(id));
    }

}
