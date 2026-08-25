package com.aaax.usecase;

import com.aaax.core.exception.BizException;
import com.aaax.core.utils.IdSplitter;
import com.aaax.core.utils.RedisUtil;
import com.aaax.config.redis.RedisKey;
import com.aaax.entity.dto.request.CreateSystemConfigurationRequestDto;
import com.aaax.entity.dto.response.GetSystemConfigurationRequestDto;
import com.aaax.entity.enu.SystemConfigurationScope;
import com.aaax.entity.po.configuration.SystemConfiguration;
import com.aaax.exception.response.SystemConfigurationErrorResponse;
import com.aaax.repository.SystemConfigurationRepository;
import com.aaax.service.DtoWrapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Runtime lookup + util-style CRUD for {@code system_configuration}
 * (list / create / update / activate / inactivate / hard-delete).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemConfigurationUseCase {

    private final SystemConfigurationRepository systemConfigurationRepository;
    private final RedisUtil redisUtil;

    // ---- CRUD (admin / util-ref-data style) ----

    public List<GetSystemConfigurationRequestDto> list(String query) {
        String q = StringUtils.trimToEmpty(query).toLowerCase(Locale.ROOT);
        return systemConfigurationRepository.findAll().stream()
                .filter(row -> matchesQuery(row, q))
                .sorted(Comparator
                        .comparing(SystemConfiguration::getIsActive, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(SystemConfiguration::getTarget, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(SystemConfiguration::getScope, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(DtoWrapper::getSystemConfiguration)
                .toList();
    }

    public GetSystemConfigurationRequestDto getById(String id) {
        return DtoWrapper.getSystemConfiguration(requireById(id));
    }

    @Transactional
    public GetSystemConfigurationRequestDto create(CreateSystemConfigurationRequestDto dto) {
        String target = normalizeTarget(dto.getTarget());
        String scope = normalizeScope(dto.getScope());
        if (systemConfigurationRepository.existsByTargetAndScope(target, scope)) {
            throw new BizException(SystemConfigurationErrorResponse.SYC0002, Map.of("target", target, "scope", scope));
        }
        SystemConfiguration systemConfiguration = SystemConfiguration.builder()
                .name(StringUtils.trimToNull(dto.getName()))
                .target(target)
                .scope(scope)
                .value(dto.getValue())
                .build();
        systemConfiguration = systemConfigurationRepository.save(systemConfiguration);
        return DtoWrapper.getSystemConfiguration(systemConfiguration);
    }

    /**
     * Full replace of mutable fields (name/target/scope/value). Persists and busts cache.
     */
    @Transactional
    public GetSystemConfigurationRequestDto update(String id, CreateSystemConfigurationRequestDto putDto) {
        SystemConfiguration systemConfiguration = requireById(id);
        String previousTarget = systemConfiguration.getTarget();
        String previousScope = systemConfiguration.getScope();

        String nextTarget = putDto.getTarget() != null ? normalizeTarget(putDto.getTarget()) : systemConfiguration.getTarget();
        String nextScope = putDto.getScope() != null ? normalizeScope(putDto.getScope()) : systemConfiguration.getScope();

        if (!StringUtils.equals(previousTarget, nextTarget) || !StringUtils.equals(previousScope, nextScope)) {
            Optional<SystemConfiguration> clash = systemConfigurationRepository.findByTargetAndScope(nextTarget, nextScope);
            if (clash.isPresent() && !clash.get().getId().equals(systemConfiguration.getId())) {
                throw new BizException(SystemConfigurationErrorResponse.SYC0002, Map.of("target", nextTarget, "scope", nextScope));
            }
        }

        if (putDto.getName() != null) {
            systemConfiguration.setName(StringUtils.trimToNull(putDto.getName()));
        }
        systemConfiguration.setTarget(nextTarget);
        systemConfiguration.setScope(nextScope);
        if (putDto.getValue() != null) {
            systemConfiguration.setValue(putDto.getValue());
        }
        systemConfiguration = systemConfigurationRepository.save(systemConfiguration);

        clearCache(previousTarget, previousScope);
        clearCache(nextTarget, nextScope);
        return DtoWrapper.getSystemConfiguration(systemConfiguration);
    }

    @Transactional
    public GetSystemConfigurationRequestDto setActive(String id, boolean isActive) {
        SystemConfiguration systemConfiguration = requireById(id);
        systemConfiguration.setIsActive(isActive);
        systemConfiguration = systemConfigurationRepository.save(systemConfiguration);
        clearCache(systemConfiguration.getTarget(), systemConfiguration.getScope());
        return DtoWrapper.getSystemConfiguration(systemConfiguration);
    }

    @Transactional
    public GetSystemConfigurationRequestDto deleteById(String id) {
        SystemConfiguration systemConfiguration = requireById(id);
        GetSystemConfigurationRequestDto response = DtoWrapper.getSystemConfiguration(systemConfiguration);
        systemConfigurationRepository.delete(systemConfiguration);
        clearCache(systemConfiguration.getTarget(), systemConfiguration.getScope());
        return response;
    }

    // ---- Runtime query (existing) ----

    public GetSystemConfigurationRequestDto query(String target, String scope) {
        List<SystemConfiguration> systemConfigurations = systemConfigurationRepository.findAllByTarget(target.toUpperCase());
        if (systemConfigurations.isEmpty()) {
            throw new BizException(SystemConfigurationErrorResponse.SYC0001, "Invalid [target and scope]. =>".concat(target));
        }
        if (StringUtils.isNotBlank(scope)) {
            Optional<SystemConfiguration> scopeSystemConfiguration = systemConfigurations.stream()
                    .filter(s -> s.getScope().equals(scope))
                    .findFirst();
            if (scopeSystemConfiguration.isPresent()) {
                log.info("===== uaa system config query: target: {}, scope: {}, value: {}", target, scope, scopeSystemConfiguration.get().getValue());
                return DtoWrapper.getSystemConfiguration(scopeSystemConfiguration.get());
            }
        }

        Optional<SystemConfiguration> systemConfiguration = systemConfigurations.stream()
                .filter(s -> s.getScope().equalsIgnoreCase(SystemConfigurationScope.GLOBAL.name()))
                .findFirst();
        if (systemConfiguration.isPresent()) {
            log.info("===== uaa system config query: target: {}, scope: {}, value: {}", target, scope, systemConfiguration.get().getValue());
            return DtoWrapper.getSystemConfiguration(systemConfiguration.get());
        }
        throw new BizException(SystemConfigurationErrorResponse.SYC0001, "Invalid [target / scope].");
    }

    public GetSystemConfigurationRequestDto myConfigurations(String target, String scope) {
        String redisKey = redisKey(target, scope);
        return redisUtil.getOrLoad(redisKey, GetSystemConfigurationRequestDto.class,
                () -> loadAndCache(target, scope, redisKey));
    }

    public GetSystemConfigurationRequestDto myConfigurations(String target) {
        return this.myConfigurations(target, "GLOBAL");
    }

    public Optional<SystemConfiguration> getOptionalSystemConfig(String target, String scope) {
        Optional<SystemConfiguration> result = systemConfigurationRepository.findByTargetAndScope(target, scope);
        if (result.isEmpty()) {
            return systemConfigurationRepository.findByTargetAndScope(target, "GLOBAL");
        }
        return result;
    }

    // ---- helpers ----

    private GetSystemConfigurationRequestDto loadAndCache(String target, String scope, String redisKey) {
        SystemConfiguration systemConfiguration = systemConfigurationRepository.findByTargetAndScope(target, scope)
                .orElseThrow(() -> new BizException(SystemConfigurationErrorResponse.SYC0001, Map.of("target", target, "scope", scope)));
        redisUtil.set(redisKey, systemConfiguration, 300);
        return DtoWrapper.getSystemConfiguration(systemConfiguration);
    }

    private SystemConfiguration requireById(String id) {
        Long numericId = IdSplitter.splitToLong(id);
        return systemConfigurationRepository.findById(numericId)
                .orElseThrow(() -> new BizException(SystemConfigurationErrorResponse.SYC0001, Map.of("id", id)));
    }

    private void clearCache(String target, String scope) {
        if (StringUtils.isBlank(target) || StringUtils.isBlank(scope)) {
            return;
        }
        try {
            redisUtil.delete(redisKey(target, scope));
        } catch (Exception ex) {
            log.warn("Failed to clear system-configuration cache target={} scope={}: {}", target, scope, ex.getMessage());
        }
    }

    private static String redisKey(String target, String scope) {
        return RedisKey.UAA_SYSTEM_CONFIGURATION.getKey().concat(target.concat(":").concat(scope));
    }

    private static String normalizeTarget(String target) {
        if (StringUtils.isBlank(target)) {
            throw new BizException(SystemConfigurationErrorResponse.SYC0001, "target is required");
        }
        return target.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeScope(String scope) {
        if (StringUtils.isBlank(scope)) {
            return SystemConfigurationScope.GLOBAL.name();
        }
        return scope.trim();
    }

    private static boolean matchesQuery(SystemConfiguration row, String q) {
        if (StringUtils.isBlank(q)) {
            return true;
        }
        return contains(row.getName(), q)
                || contains(row.getTarget(), q)
                || contains(row.getScope(), q)
                || contains(String.valueOf(row.getValue()), q);
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }
}
