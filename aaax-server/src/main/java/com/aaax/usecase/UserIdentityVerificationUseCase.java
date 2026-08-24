package com.aaax.usecase;

import com.aaax.core.constant.Common;
import com.aaax.core.constant.enu.*;
import com.aaax.core.constant.enu.Locale;
import com.aaax.core.entity.dto.uaa.response.GetUserMetricsResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserProfileResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserVerificationResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.BaseNotificationEvent;
import com.aaax.core.kafka.CreateMassNotificationRequestDto;
import com.aaax.core.response.PaginationDto;
import com.aaax.core.utils.*;
import com.aaax.entity.dto.request.UserIdentityVerificationResultRequestDto;
import com.aaax.entity.dto.response.GetSystemConfigurationRequestDto;
import com.aaax.entity.enu.UserProfileType;
import com.aaax.entity.po.user_management.UserProfile;
import com.aaax.entity.po.user_verification.UserVerification;
import com.aaax.exception.response.UserProfileErrorResponse;
import com.aaax.exception.response.UserVerificationErrorResponse;
import com.aaax.ext.api.client.idv.IdvApiClient;
import com.aaax.ext.api.client.idv.dto.CreateIdvRequestDto;
import com.aaax.ext.api.client.idv.dto.CreateIdvResponseDto;
import com.aaax.repository.UserProfileRepository;
import com.aaax.repository.UserVerificationRepository;
import com.aaax.service.DtoWrapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.aaax.core.kafka.enu.KafkaTopic.NOTIFICATION_MASS;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserIdentityVerificationUseCase {

    private final IdvApiClient idvApiClient;
    private final UserProfileRepository userProfileRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final SystemConfigurationUseCase systemConfigurationUseCase;
    private final UserMetricsUseCase userMetricsUseCase;
    private final KafkaUtil kafkaUtil;
    @Value("${webhook.uaa.domain}")
    private String uaaWebhookDomain;
    @Value("${config.microservice.timezone:UTC}")
    private String timezone;

    @Transactional
    public void updateIdvResults(UserIdentityVerificationResultRequestDto dto) {
        String userId = JwtUtil.userId();
        this.beforeUpdateIdvResults_validations(userId);

        String callbackUrl = uaaWebhookDomain.concat("webhooks/uaa/").concat(userId).concat("/activations");
        CreateIdvRequestDto idvRequestDto = CreateIdvRequestDto.builder()
                .accountId(dto.getAccountId())
                .workflowExecutionId(dto.getWorkflowExecutionId())
                .callbackUrl(callbackUrl)
                .metadata(Map.of("userId", userId))
                .build();
        log.info("==== user activations => before create idv request ：{}", idvRequestDto);
        CreateIdvResponseDto idvResponseDto = RetrofitCallHandler.execute(idvApiClient.createIdvRequest(idvRequestDto));
        log.info("==== user activations => after create idv response ：{}", idvResponseDto);

        // === create association
        UserVerification userVerification = UserVerification.builder()
                .userId(Long.valueOf(userId))
                .extIdentifier(idvResponseDto.getId())
                .detail(Map.of(
                        "idvRequest", Map.of(
                                "client", "idvApiClient",
                                "request", idvRequestDto,
                                "response", idvResponseDto),
                        "sourceSystem", dto.getSourceSystem()
                ))
                .status(UserVerificationStatus.PENDING_CALLBACK)
                .build();
        userVerification = userVerificationRepository.save(userVerification);
        log.info("==== userVerification => {}", userVerification);
    }

    private void beforeUpdateIdvResults_validations(String userId) {
        UserProfile userProfile = userProfileRepository.findByUserIdAndType(Long.valueOf(IdSplitter.split(userId)), UserProfileType.DEFAULT.name()).orElseThrow(() -> new BizException(UserProfileErrorResponse.UPR0001, Map.of("userId", userId)));
        UserVerificationStatus status = _getIdvStatus(userProfile);
        switch (status) {
            case SUSPENDED ->
                    throw new BizException(UserVerificationErrorResponse.UVR0429, "Sorry Please Contact Admin for your account [%s] with status [%s]: ".formatted(userId, status));
        }
    }

    private UserVerificationStatus _getIdvStatus(UserProfile userProfile) {
        Map metadata = JSONUtil.convertFromObject(userProfile.getContext(), Map.class);
        Map verification = JSONUtil.convertFromObject(metadata.get("verification"), Map.class);
        return UserVerificationStatus.get((String) verification.get("idvStatus"));
    }

    @Transactional
    public GetUserProfileResponseDto complete(String userId, Object data) {
        Map _data = JSONUtil.convertFromObject(data, Map.class);
        // 1. user verification
        String idvStatus = UserVerificationStatus.PENDING.name(); // DEFAULT
        String sourceSystem = Common.GLOBAL;
        Optional<UserVerification> isExistedUserVerification = userVerificationRepository.findByUserIdAndExtIdentifier(Long.valueOf(IdSplitter.split(userId)), (String) _data.get("id"));
        if (isExistedUserVerification.isPresent()) {
            Map detail = JSONUtil.convertFromObject(isExistedUserVerification.get().getDetail(), Map.class);
            detail.put("idvCallback", Map.of("response", _data));
            isExistedUserVerification.get().setDetail(detail);
            // case when [_data.status]
            switch ((String) _data.getOrDefault("status", "NA")) {
                // TODO: ask joe for status enums.
                case ("CLOSED") -> {
                    UserVerificationStatus finalStatus = this.idvDecision(_data);
                    isExistedUserVerification.get().setStatus(finalStatus);
                }
                case ("EXPIRED") -> isExistedUserVerification.get().setStatus(UserVerificationStatus.INVALID_CALLBACK);
                default -> isExistedUserVerification.get().setStatus(UserVerificationStatus.REJECTED);
            }
            userVerificationRepository.save(isExistedUserVerification.get());

            // reset params
            idvStatus = isExistedUserVerification.get().getStatus().name();
            sourceSystem = (String) detail.getOrDefault("sourceSystem", Common.GLOBAL);
        }

        idvStatus = this.secondValidations_idvStatus(userId, idvStatus, sourceSystem);  // 二次較驗
        // 2. user profile
        UserProfile userProfile = userProfileRepository.findByUserIdAndType(Long.valueOf(IdSplitter.split(userId)), UserProfileType.DEFAULT.name()).orElseThrow(() -> new BizException(UserProfileErrorResponse.UPR0001, Map.of("userId", userId)));
        Map metadata = JSONUtil.convertFromObject(userProfile.getContext(), Map.class);
        Map verification = JSONUtil.convertFromObject(metadata.get("verification"), Map.class);
        String existingStatus = (String) verification.getOrDefault("idvStatus", "NA");

        // bypass when status is VERIFIED & callback is INVALID_CALLBACK
        if (existingStatus.equalsIgnoreCase("VERIFIED") || idvStatus.equals(UserVerificationStatus.INVALID_CALLBACK.name())) {
            return DtoWrapper.getUserProfileResponseDto(userProfile);
        }

        verification.put("idvStatus", idvStatus);
        metadata.put("verification", verification);
        userProfile.setContext(metadata);
        userProfile = userProfileRepository.save(userProfile);
        this.afterVerification(userId, idvStatus, sourceSystem);
        return DtoWrapper.getUserProfileResponseDto(userProfile);
    }

    private UserVerificationStatus idvDecision(Map data) {
        Map detailResult = (Map) data.getOrDefault("detail", new HashMap<>());
        String liveness = (String) detailResult.getOrDefault("liveness", "NA");
        String similarity = (String) detailResult.getOrDefault("similarity", "NA");

        // UserVerificationStatus.VERIFIED
        if ("PASSED".equalsIgnoreCase(liveness) && "PASSED".equalsIgnoreCase(similarity)) {
            return UserVerificationStatus.VERIFIED;
        }

        return UserVerificationStatus.REJECTED;
    }


    private String secondValidations_idvStatus(String userId, String idvStatus, String sourceSystem) {
        GetSystemConfigurationRequestDto config = systemConfigurationUseCase.query("IDV_FAILURE_COUNTER", sourceSystem);
        int counter = Integer.parseInt(String.valueOf(config.getValue()));
        List<UserVerification> userVerifications = userVerificationRepository.findByUserId(Long.valueOf(IdSplitter.split(userId)));
        long failCount = userVerifications.stream()
                .filter(userVerification -> "REJECTED".equalsIgnoreCase(userVerification.getStatus().name())) // only count rejected.
                .count();
        if (failCount >= counter) {
            return UserVerificationStatus.SUSPENDED.name();
        }
        return idvStatus;
    }

    public GetUserVerificationResponseDto patchStatuses(String id, UserVerificationStatus status, String systemSource) {
        UserVerification userVerification = userVerificationRepository.findById(Long.valueOf(IdSplitter.split(id))).orElseThrow(() -> new BizException(UserVerificationErrorResponse.UVR0001, Map.of("id", id)));
        userVerification.setStatus(status);
        userVerification = userVerificationRepository.save(userVerification);
        this.afterVerification(id, status.name(), systemSource);
        return DtoWrapper.getUserVerificationResponseDto(userVerification);
    }

    public PaginationDto.PaginationDtoBuilder getAll(Pageable pageable, String startDt, String endDt) {
        Instant _startDt = StringUtils.isBlank(startDt) ? InstantUtil.parse(InstantUtil.EARLIEST_DATE) : InstantUtil.parse_tz(startDt, timezone);
        Instant _endDt = StringUtils.isBlank(endDt) ? InstantUtil.parse(InstantUtil.NEVER_EXPIRED) : InstantUtil.parse_tz(endDt, timezone);
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber() - 1, pageable.getPageSize(),
                pageable.getSort().iterator().next().getDirection(),
                pageable.getSort().iterator().next().getProperty()
        );
        Specification<UserVerification> specification = Specification.where(null);
        specification = specification.and(((root, query, builder) -> builder.between(root.get("createDt"), _startDt, _endDt.plusSeconds(1)))); // in-case last second. of 23:59:59
        Page<UserVerification> userVerifications = userVerificationRepository.findAll(specification, pageRequest);
        log.info("-- ================ Fetch ALL userVerifications =======================");
        List<GetUserVerificationResponseDto> data = userVerifications.getContent().stream()
                .map(DtoWrapper::getUserVerificationResponseDto)
                .collect(Collectors.toList());
        return DtoWrapper.getListWithPaginationResponseDto(data, userVerifications);
    }

    public GetUserVerificationResponseDto getOne(String id) {
        UserVerification userVerification = userVerificationRepository.findById(Long.valueOf(IdSplitter.split(id))).orElseThrow(() -> new BizException(UserVerificationErrorResponse.UVR0001, Map.of("id", id)));
        return DtoWrapper.getUserVerificationResponseDto(userVerification);
    }

    public List<GetUserVerificationResponseDto> myVerifications(String userId) {
        List<UserVerification> userVerifications = userVerificationRepository.findByUserId(Long.valueOf(IdSplitter.split(userId)));
        return userVerifications.stream()
                .map(DtoWrapper::getUserVerificationResponseDto)
                .sorted(Comparator.comparing(GetUserVerificationResponseDto::getCreateDt).reversed())
                .collect(Collectors.toList());
    }

    public void afterVerification(String userId, String status, String sourceSystem) {
        switch (sourceSystem) {
            case "RENTEASE" -> {
                GetUserMetricsResponseDto userMetric = userMetricsUseCase.execute(userId, sourceSystem);
                switch (status) {
                    case "VERIFIED" ->
                            this.triggerNotification(userMetric, "rentease.idv.verified", new HashMap(), sourceSystem);
                }
            }
        }
    }

    public void triggerNotification(GetUserMetricsResponseDto userMetric, String notificationTemplateName, Map param, String systemSource) {
        Map profile = JSONUtil.convertFromObject(userMetric.getProfile().getContext(), Map.class);
        Map preference = JSONUtil.convertFromObject(userMetric.getPreference().getContext(), Map.class);
        Map localizations = (Map) preference.getOrDefault("localizations", new HashMap<>());
        Locale locale = Locale.get((String) localizations.getOrDefault("selected", "ja"));

        List<NotificationChannel> channels = new ArrayList<>();
        Map notification = (Map) preference.getOrDefault("notifications", new HashMap<>());
        Map platforms = (Map) notification.getOrDefault("platforms", new HashMap<>());
        List<Map<String, Object>> all = (List<Map<String, Object>>) platforms.get("all");
        if (all != null) {
            for (Map<String, Object> platform : all) {
                NotificationChannel channel = NotificationChannel.get((String) platform.get("name"));
                boolean isEnabled = (Boolean) platform.get("isEnabled");
                if (isEnabled) {
                    channels.add(channel);
                }
            }
        }

        Map<String, Object> parameterMap = Map.of(
                "lastName", (String) profile.getOrDefault("lastName", ""),
                "firstName", (String) profile.getOrDefault("firstName", ""),
                "email", (String) profile.getOrDefault("email", ""),
                "userId", userMetric.getUser().getId()  // Mandatory field for mapping the app user
        );
        if (!param.isEmpty()) {
            parameterMap.putAll(param);
        }

        for (NotificationChannel channel : channels) {
            String to = null;
            switch (channel) {
                case APP_PUSH -> {
                    if (userMetric.getDevice().getContext().stream().findFirst().isEmpty()) {
                        // no context found then skip
                        continue;
                    }
                    to = userMetric.getDevice().getContext().stream().findFirst().get().getToken().getOrDefault("fcm", "NA");
                }
            }
            if (Optional.ofNullable(to).isEmpty()) {
                log.info("======== leasing trigger notification ======== not found in user metric userId: {}", userMetric.getUser().getId());
                continue;
            }
            CreateMassNotificationRequestDto notificationRequestDto = CreateMassNotificationRequestDto.builder()
                    .notificationTemplateName(notificationTemplateName)
                    .channels(List.of(channel))
                    .parameterMap(parameterMap)
                    .locale(List.of(locale))
                    .systemInvoker(systemSource)
                    .notificationAction(NotificationAction.REALTIME)
                    .notificationFrequency(NotificationFrequency.ONE_OFF)
                    .recipients(List.of(BaseNotificationEvent.builder()
                            .to(to)
                            .build()))
                    .build();
            kafkaUtil.send(NOTIFICATION_MASS, notificationRequestDto);
        }
    }
}
