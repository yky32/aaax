package com.aaax.server.endpoint.aaax_config;

import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.utils.ValidationUtil;
import com.aaax.server.entity.dto.request.CreateSystemConfigurationRequestDto;
import com.aaax.server.entity.dto.response.GetSystemConfigurationRequestDto;
import com.aaax.server.usecase.SystemConfigurationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * System configuration CRUD (util {@code /ref-data} style) + runtime lookup by target.
 *
 * <pre>
 * POST   /system-configurations
 * GET    /system-configurations?query=
 * GET    /system-configurations/id/{id}
 * PUT    /system-configurations/id/{id}
 * PUT    /system-configurations/activate/{id}
 * DELETE /system-configurations/inactivate/{id}
 * DELETE /system-configurations/id/{id}
 * GET    /system-configurations/t/{target}?scope=
 * POST   /system-configurations/{id}   (legacy update)
 * </pre>
 */
@RestController
@RequestMapping("/system-configurations")
@RequiredArgsConstructor
@Slf4j
public class SystemConfigurationEndpoint {

    private final SystemConfigurationUseCase systemConfigurationUseCase;

    @PostMapping
    public Result<GetSystemConfigurationRequestDto> create(@Valid @RequestBody CreateSystemConfigurationRequestDto dto) {
        ValidationUtil.nonEmptyNonNull(dto.getName(), "name");
        ValidationUtil.nonEmptyNonNull(dto.getTarget(), "target");
        return R.success(systemConfigurationUseCase.create(dto));
    }

    @GetMapping
    public Result<List<GetSystemConfigurationRequestDto>> list(
            @RequestParam(value = "query", required = false) String query) {
        return R.success(systemConfigurationUseCase.list(query));
    }

    @GetMapping("/id/{id}")
    public Result<GetSystemConfigurationRequestDto> getById(@PathVariable String id) {
        return R.success(systemConfigurationUseCase.getById(id));
    }

    @PutMapping("/id/{id}")
    public Result<GetSystemConfigurationRequestDto> updateById(
            @PathVariable String id,
            @RequestBody CreateSystemConfigurationRequestDto putDto) {
        return R.success(systemConfigurationUseCase.update(id, putDto));
    }

    /**
     * Soft-delete: set isActive=false. Reverse with {@link #activate(String)}.
     */
    @DeleteMapping("/inactivate/{id}")
    public Result<GetSystemConfigurationRequestDto> inactivate(@PathVariable String id) {
        return R.success(systemConfigurationUseCase.setActive(id, false));
    }

    @PutMapping("/activate/{id}")
    public Result<GetSystemConfigurationRequestDto> activate(@PathVariable String id) {
        return R.success(systemConfigurationUseCase.setActive(id, true));
    }

    /** Hard-delete by id. */
    @DeleteMapping("/id/{id}")
    public Result<GetSystemConfigurationRequestDto> deleteById(@PathVariable String id) {
        return R.success(systemConfigurationUseCase.deleteById(id));
    }

    @GetMapping("/t/{target}")
    public Result<GetSystemConfigurationRequestDto> get(
            @PathVariable String target,
            @RequestParam(required = false) String scope
    ) {
        return R.success(systemConfigurationUseCase.query(target, scope));
    }

    /** Legacy update path (same behaviour as PUT /id/{id}). */
    @PostMapping("/{id}")
    public Result<GetSystemConfigurationRequestDto> update(
            @PathVariable String id,
            @RequestBody CreateSystemConfigurationRequestDto putDto
    ) {
        return R.success(systemConfigurationUseCase.update(id, putDto));
    }
}
