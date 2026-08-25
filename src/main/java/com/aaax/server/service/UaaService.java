package com.aaax.server.service;

import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.entity.dto.uaa.response.GetUserMetricsResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserVerificationResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.PaginationDto;
import com.aaax.core.utils.IdSplitter;
import com.aaax.core.utils.InstantUtil;
import com.aaax.core.utils.RetrofitCallHandler;
import com.aaax.core.utils.jpa.JpaSearchFieldMetadata;
import com.aaax.core.utils.jpa.JpaUtil;
import com.aaax.server.entity.dto.response.GetTenantRoleWithRouteResponseDto;
import com.aaax.server.entity.enu.UaaAspect;
import com.aaax.server.entity.po.UserRoute;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.entity.po.user_management.UserProfile;
import com.aaax.server.exception.response.UaaErrorResponse;
import com.aaax.server.ext.api.client.tenant.TenantApiClient;
import com.aaax.server.repository.AuthenticationRepository;
import com.aaax.server.repository.UserRepository;
import com.aaax.server.repository.UserRouteRepository;
import com.aaax.server.usecase.UserMetricsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UaaService {
    private final UserRouteRepository userRouteRepository;
    private final UserRepository userRepository;
    private final AuthenticationRepository authenticationRepository;
    private final AuthenticationService authenticationService;
    private final ResourceLoader resourceLoader;
    private final TenantApiClient tenantApiClient;
    private final CommonService commonService;
    private final UserProfileService userProfileService;
    @Lazy
    @Autowired
    private UserMetricsUseCase userMetricsUseCase;
    @Value("${config.microservice.timezone:UTC}")
    private String timezone;

    public Authentication getByUsername(String username) {
        return authenticationService.findValidRecordsByDynamicIdentifier(username);
    }

    @Transactional(readOnly = true)
    public GetUserResponseDto me(String userId) {
        return this.get(Long.valueOf(userId));
    }

    @Transactional(readOnly = true)
    public GetUserResponseDto get(Long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            return GetUserResponseDto.builder()
                    .id("NA")
                    .username("NA")
                    .build();
        }
        List<Authentication> authentications = optionalUser.get().getAuthentications();
        return DtoWrapper.getUserResponseDto(optionalUser.get(), authentications);
    }

    @Transactional(readOnly = true)
    public PaginationDto.PaginationDtoBuilder getAll(Pageable pageable, String startDt, String endDt, String tenantId, List<String> sourceSystems, List<String> aspects, List<String> ids,
                                                     String searchText, String namesQuery, String emailQuery) {
        // specification
        if (StringUtils.isBlank(startDt)) {
            startDt = InstantUtil.EARLIEST_DATE;
        }
        if (StringUtils.isBlank(endDt)) {
            endDt = InstantUtil.NEVER_EXPIRED;
        }

        Instant _startDt = InstantUtil.parse_tz(startDt, timezone);
        Instant _endDt = InstantUtil.parse_tz(endDt, timezone);
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber() - 1, pageable.getPageSize(),
                pageable.getSort().iterator().next().getDirection(),
                pageable.getSort().iterator().next().getProperty()
        );
        Specification<User> specification;
        specification = Specification.where(((root, query, builder) -> builder.between(root.get("createDt"), _startDt, _endDt)));
        if (!StringUtils.isEmpty(searchText)) {
            List<JpaSearchFieldMetadata> filters = JpaUtil.getJpaSearchFieldMetadata("jpa_specification/get_users_specification.json", resourceLoader);
            log.info("-- List<JpaSearchFieldMetadata> for [Specification<CheckoutSession>] => {}", filters);
            specification = specification.and((Specification<User>) JpaUtil.fuzzySearchSpecification(searchText, filters));
            List<UserProfile> userProfiles = userProfileService.search(searchText);
            specification = specification.and(((root, query, builder) -> root.get("id").in(userProfiles.stream().map(UserProfile::getUserId).toList())));
        }

        if (!StringUtils.isEmpty(namesQuery)) {
            List<UserProfile> userProfiles = userProfileService.searchNames(namesQuery);
            specification = specification.and(((root, query, builder) -> root.get("id").in(userProfiles.stream().map(UserProfile::getUserId).toList())));
        }

        if (!StringUtils.isEmpty(emailQuery)) {
            List<JpaSearchFieldMetadata> filters = JpaUtil.getJpaSearchFieldMetadata("jpa_specification/get_users_email_specification.json", resourceLoader);
            log.info("-- List<JpaSearchFieldMetadata> for [Specification<CheckoutSession>] => username {}", filters);
            specification = specification.and((Specification<User>) JpaUtil.fuzzySearchSpecification(emailQuery, filters));
            List<UserProfile> userProfiles = userProfileService.searchEmail(emailQuery);
            specification = specification.and(((root, query, builder) -> root.get("id").in(userProfiles.stream().map(UserProfile::getUserId).toList())));
        }

        List<String> _sourceSystems = Optional.ofNullable(sourceSystems).isEmpty() ? new ArrayList<>() : sourceSystems;
        if (!_sourceSystems.isEmpty()) {
            _sourceSystems.forEach(commonService::isValidSourceSystem);
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.isTrue(
                            criteriaBuilder.function("jsonb_exists_any",
                                    Boolean.class,
                                    root.get("sourceSystemTags"),
                                    criteriaBuilder.literal(sourceSystems.toArray(new String[0]))
                            )
                    )
            );
        }
        if (!StringUtils.isEmpty(tenantId)) {
            List<GetTenantRoleWithRouteResponseDto> trrList = RetrofitCallHandler.execute(tenantApiClient.getTenantRoleRouteByTenantId(tenantId));
            List<UserRoute> userRoutes = userRouteRepository.findAllByTenantRoleRouteIdIn(trrList.stream().map(trr -> Long.valueOf(trr.getId())).toList());
            specification = specification.and((root, query, builder) -> root.get("id").in(userRoutes.stream().map(UserRoute::getUserId).toList()));
        }

        List<String> _ids = Optional.ofNullable(ids).orElse(List.of());

        if (!_ids.isEmpty()) {
            var findId = _ids.stream().map(uid -> Long.valueOf(IdSplitter.split(uid))).toList();
            specification = specification.and((root, query, builder) -> root.get("id").in(findId));
        }

        Page<User> users = userRepository.findAll(specification, pageRequest);

        List<UserProfile> userProfiles = userProfileService.getByUserIds(users.getContent().stream().map(User::getId).toList());

        List<GetUserResponseDto> data = users.getContent().stream()
                .map(user -> {
                    GetUserResponseDto userResponseDto = DtoWrapper.getUserResponseDto(user, user.getAuthentications());
                    GetUserMetricsResponseDto execute = userMetricsUseCase.execute(userResponseDto.getId(), null);
                    List<String> _aspects = Optional.ofNullable(aspects).isEmpty() ? new ArrayList<>() : aspects;
                    for (String aspect : _aspects) {
                        Object aspectValue = this.attachAdditionalAspects(aspect, execute);
                        if (aspectValue == null) continue;
                        Map<String, Object> userAspect = Optional.ofNullable(userResponseDto.getMetrics()).orElse(new HashMap<>());
                        userAspect.put(aspect.toLowerCase(), aspectValue);
                        userResponseDto.setMetrics(userAspect);
                    }
                    userProfiles.stream().filter(up -> up.getUserId().equals(user.getId())).findFirst().ifPresent(userResponseDto::setUserProfile);
                    return userResponseDto;
                })
                .toList();
        return DtoWrapper.getListWithPaginationResponseDto(data, users);
    }


    @Transactional(readOnly = true)
    public GetUserResponseDto getOne(String id, List<String> aspects, String sourceSystem) {
        Optional<User> optionalUser = userRepository.findById(Long.valueOf(IdSplitter.split(id)));
        if (optionalUser.isEmpty()) {
            return GetUserResponseDto.builder()
                    .id("NA")
                    .username("NA")
                    .build();
        }
        List<Authentication> authentications = optionalUser.get().getAuthentications();
        GetUserResponseDto userResponseDto = DtoWrapper.getUserResponseDto(optionalUser.get(), authentications);
        GetUserMetricsResponseDto execute = userMetricsUseCase.execute(userResponseDto.getId(), sourceSystem);
        List<String> _aspects = Optional.ofNullable(aspects).isEmpty() ? new ArrayList<>() : aspects;
        for (String aspect : _aspects) {
            Object aspectValue = this.attachAdditionalAspects(aspect, execute);
            if (aspectValue == null) continue;
            Map<String, Object> userAspect = Optional.ofNullable(userResponseDto.getMetrics()).orElse(new HashMap<>());
            userAspect.put(aspect.toLowerCase(), aspectValue);
            userResponseDto.setMetrics(userAspect);
        }
        return userResponseDto;
    }

    private Object attachAdditionalAspects(String aspect, GetUserMetricsResponseDto execute) {
        return switch (aspect) {
            case (UaaAspect.PREFERENCE) -> execute.getPreference();
            case (UaaAspect.PROFILE) -> execute.getProfile();
            case (UaaAspect.DEVICE) -> execute.getDevice();
            case (UaaAspect.VERIFICATION) ->
                    execute.getVerifications().stream().map(GetUserVerificationResponseDto::getDetail);
            default ->
                    throw new BizException(UaaErrorResponse.UAA0400, "Invalid [a] parameters %s of %s".formatted(aspect, UaaAspect.USERS));
        };
    }

    public GetUserResponseDto getByIdentifierType(String identifier, String identifierType) {
        switch (LoginType.get(identifierType)) {
            case USERNAME -> {
                Authentication authentication = getByUsername(identifier);
                return DtoWrapper.getUserResponseDto(authentication.getUser(), List.of());
            }
        }
        throw new BizException(UaaErrorResponse.UAA0001, Map.of("id", identifier));
    }

    public User getUserFromIdentifier(String identifier) {
        Authentication authentication = getByUsername(identifier);
        return authentication.getUser();
    }

    public User getById(String id) {
        return userRepository.findById(Long.valueOf(IdSplitter.split(id))).orElseThrow(() -> new BizException(UaaErrorResponse.UAA0001, Map.of("id", id)));
    }

    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new BizException(UaaErrorResponse.UAA0001, Map.of("id", id)));
    }

    public User getByExtReference(String extReferenceKey, String extReferenceValue) {
        return userRepository.findByExtRef(extReferenceKey, extReferenceValue).orElseThrow(() -> new BizException(UaaErrorResponse.UAA0001, Map.of("extReferenceKey", extReferenceKey, "extReferenceValue", extReferenceValue)));
    }

    @Transactional(readOnly = true)
    public GetUserResponseDto getOne(String id) {
        return this.get(Long.valueOf(IdSplitter.split(id)));
    }
}
