package com.aaax.server.usecase;

import com.aaax.core.common.jsonfield.PermissionMetadata;
import com.aaax.core.constant.enu.uaa.Authorities;
import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.event.UserPermissionMutatedEvent;
import com.aaax.core.response.PaginationDto;
import com.aaax.core.utils.*;
import com.aaax.server.config.redis.RedisKey;
import com.aaax.server.entity.dto.json_context.PermissionDbMetadata;
import com.aaax.server.entity.dto.request.AssignUserRoleRequestDto;
import com.aaax.server.entity.dto.request.CreateRbacTemplateRequestDto;
import com.aaax.server.entity.dto.response.GetRbacTemplateResponseDto;
import com.aaax.server.entity.dto.response.GetUserPermissionResponseDto;
import com.aaax.server.entity.po.user_management.UserPermission;
import com.aaax.server.entity.po.rbac.RbacTemplate;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.exception.response.RbacTemplateErrorResponse;
import com.aaax.server.repository.RbacTemplateRepository;
import com.aaax.server.repository.UserPermissionRepository;
import com.aaax.server.service.DtoWrapper;
import com.aaax.server.service.UaaService;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccessControlUseCase {

    @Value("${aaax.config.microservice.timezone:UTC}")
    private String timezone;

    private final RbacTemplateRepository rbacTemplateRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final RedisUtil redisUtil;
    private final UaaService uaaService;
    private final ResourceLoader resourceLoader;

    @Transactional
    public GetRbacTemplateResponseDto createRbacTemplate(CreateRbacTemplateRequestDto dto) {
        Map<String, PermissionMetadata> permissions = JSONUtil.convertFromObject(dto.getPermissions(), new TypeReference<>() {});
        for (Map.Entry<String, PermissionMetadata> entry : permissions.entrySet()) {
            PermissionMetadata metadata = entry.getValue();
            authorities_replaceAll(metadata);
        }

        RbacTemplate rbacTemplate = RbacTemplate.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .permissions(new HashMap<>(permissions))
                .build();
        rbacTemplate = rbacTemplateRepository.save(rbacTemplate);
        return DtoWrapper.getRbacTemplateResponseDto(rbacTemplate);
    }

    public PaginationDto.PaginationDtoBuilder getAllRbacTemplates(Pageable pageable, String startDt, String endDt) {
        // ====== specification
        Instant _startDt = StringUtils.isBlank(startDt) ? InstantUtil.parse(InstantUtil.EARLIEST_DATE) : InstantUtil.parse_tz(startDt, timezone);
        Instant _endDt = StringUtils.isBlank(endDt) ? InstantUtil.parse(InstantUtil.NEVER_EXPIRED) : InstantUtil.parse_tz(endDt, timezone);

        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber() - 1, pageable.getPageSize(),
                pageable.getSort().iterator().next().getDirection(),
                pageable.getSort().iterator().next().getProperty()
        );

        // ====== start query
        Specification<RbacTemplate> specification;
        specification = Specification.where(((root, query, builder) -> builder.between(root.get("createDt"), _startDt, _endDt)));
        Page<RbacTemplate> logs = rbacTemplateRepository.findAll(specification, pageRequest);
        List<GetRbacTemplateResponseDto> data = logs.getContent().stream()
                .map(DtoWrapper::getRbacTemplateResponseDto)
                .toList();
        return DtoWrapper.getListWithPaginationResponseDto(data, logs);
    }

    public GetRbacTemplateResponseDto getOneRbacTemplate(String id) {
        RbacTemplate rbacTemplate = rbacTemplateRepository.findById(Long.valueOf(IdSplitter.split(id))).orElseThrow(() -> new BizException(RbacTemplateErrorResponse.RBA0001, Map.of("id", id)));
        return DtoWrapper.getRbacTemplateResponseDto(rbacTemplate);
    }

    public GetUserPermissionResponseDto assignPermissionToUser(@Valid UserPermissionMutatedEvent requestDto) {
        User user = uaaService.getById(requestDto.getUserId()); // just for verification
        Optional<UserPermission> isExistedUserPermission = userPermissionRepository.findByUserId(user.getId());
        UserPermission userPermission = isExistedUserPermission.orElseGet(() ->
                UserPermission.builder()
                        .apiVersion("1.0")
                        .userId(user.getId())
                        .actualPermissions(new HashMap<>())
                        .build());
        userPermission = userPermissionRepository.saveAndFlush(userPermission);

        Map<String, Object> actualPermissions = userPermission.getActualPermissions();
        if (actualPermissions.isEmpty()) {
            for (PermissionMetadata requestPermission : requestDto.getPermissions()) {
                authorities_replaceAll(requestPermission);
                PermissionDbMetadata _saveToDb = new PermissionDbMetadata(requestPermission);
                actualPermissions.put(requestPermission.getKey(), _saveToDb);
            }
            return getGetUserPermissionResponseDto(userPermission, actualPermissions);
        }


        for (PermissionMetadata requestPermission : requestDto.getPermissions()) {
            authorities_replaceAll(requestPermission);
            if (requestPermission.getIsOverride()) {
                PermissionDbMetadata _saveToDb = new PermissionDbMetadata(requestPermission);
                log.info("====IsOverride: {}  \n DB-Operation [{}], value: [{}]", requestPermission.getIsOverride(), requestPermission.getDbOperation(), _saveToDb);
                actualPermissions.put(requestPermission.getKey(), _saveToDb);
                continue; // quick jump
            }

            switch (requestPermission.getDbOperation()) {
                case CREATE -> {
                    PermissionMetadata existedPermission = JSONUtil.convertFromObject(actualPermissions.get(requestPermission.getKey()), new TypeReference<>() {});
                    existedPermission.setEffect(requestPermission.getEffect());
                    for (Authorities requestAuthority : requestPermission.getAuthorities()) {
                        if (!existedPermission.getAuthorities().contains(requestAuthority)) {
                            existedPermission.getAuthorities().add(requestAuthority);
                        }
                    }
                    PermissionDbMetadata _saveToDb = new PermissionDbMetadata(existedPermission);
                    log.info("====IsOverride: {}  \n DB-Operation [{}], value: [{}]", requestPermission.getIsOverride(), requestPermission.getDbOperation(), _saveToDb);
                    actualPermissions.put(requestPermission.getKey(), _saveToDb);
                }
                case UPDATE -> {
                    PermissionDbMetadata _saveToDb = new PermissionDbMetadata(requestPermission);
                    log.info("====IsOverride: {}  \n DB-Operation [{}], value: [{}]", requestPermission.getIsOverride(), requestPermission.getDbOperation(), _saveToDb);
                    actualPermissions.put(requestPermission.getKey(), _saveToDb);
                }
                case DELETE -> actualPermissions.remove(requestPermission.getKey());
            }
        }
        return getGetUserPermissionResponseDto(userPermission, actualPermissions);
    }

    private GetUserPermissionResponseDto getGetUserPermissionResponseDto(UserPermission userPermission, Map<String, Object> actualPermissions) {
        userPermission.setActualPermissions(actualPermissions);
        userPermission = userPermissionRepository.save(userPermission);
        // CLEANUP REDIS
        clearLoginAuthorizationCaches(userPermission.getUserId());
        return DtoWrapper.getUserPermissionResponseDto(userPermission);
    }

    private void clearLoginAuthorizationCaches(Long userId) {
        String userIdKey = String.valueOf(userId);
        String permissionsKey = RedisKey.LOGIN_MY_PERMISSIONS.getKey().concat(userIdKey);
        String rolesKey = RedisKey.LOGIN_MY_ROLES.getKey().concat(userIdKey);
        redisUtil.delete(permissionsKey);
        redisUtil.delete(rolesKey);
        log.info("======= Cleared login authorization caches for userId={}.", userId);
    }

    private static void authorities_replaceAll(PermissionMetadata metadata) {
        if (metadata.getAuthorities().contains(Authorities.ALL)) {
            List<Authorities> allAuthorities = Arrays.stream(Authorities.values()).filter(authority -> authority != Authorities.ALL).toList();
            metadata.setAuthorities(allAuthorities);
        }
    }

    public GetUserPermissionResponseDto assignRoleToUser(String userId, @Valid AssignUserRoleRequestDto dto) {
        Map config = ResourcesUtil.readJson("config/user_permissions_sample.json", resourceLoader, Map.class);
        User user = uaaService.getById(userId);
        Optional<UserPermission> isExistedUserPermission = userPermissionRepository.findByUserId(user.getId());
        UserPermission userPermission = isExistedUserPermission.orElseGet(() ->
                UserPermission.builder()
                        .apiVersion("1.0")
                        .userId(user.getId())
                        .actualPermissions(config)
                        .build());
        // add the roles into it.
        for (String role : dto.getRoles()) {
            userPermission.getActualPermissions().putIfAbsent(role, config.getOrDefault(role, new HashMap<>()));
        }

        // remove the redundant roles from DB object
        for (Map.Entry<String, Object> entry : userPermission.getActualPermissions().entrySet()) {
            String key = entry.getKey();
            if (!dto.getRoles().contains(key)) {
                userPermission.getActualPermissions().remove(key);
            }
        }

        userPermission = userPermissionRepository.save(userPermission);
        redisUtil.delete(RedisKey.USER_OAUTH_TOKENS.getKey().concat(String.valueOf(user.getId()))); // remove the cache
        clearLoginAuthorizationCaches(user.getId());
        return DtoWrapper.getUserPermissionResponseDto(userPermission);
    }
}

