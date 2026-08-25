package com.aaax.server.service;

import com.aaax.core.common.i18n.i18n;
import com.aaax.core.entity.dto.uaa.response.*;
import com.aaax.core.response.Pagination;
import com.aaax.core.response.PaginationDto;
import com.aaax.core.utils.JSONUtil;
import com.aaax.server.entity.dto.json_context.OtpMetadata;
import com.aaax.server.entity.dto.response.*;
import com.aaax.server.entity.po.UserRoute;
import com.aaax.server.entity.po.configuration.SystemConfiguration;
import com.aaax.server.entity.po.log.AuthenticationLog;
import com.aaax.server.entity.po.rbac.RbacTemplate;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.entity.po.user_management.UserDevice;
import com.aaax.server.entity.po.user_management.UserPermission;
import com.aaax.server.entity.po.user_management.UserPreference;
import com.aaax.server.entity.po.user_management.UserProfile;
import com.aaax.server.entity.po.user_verification.UserVerification;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DtoWrapper {

    public static GetUserResponseDto getUserResponseDto(User user, List<Authentication> authentications) {
        List<UserLoginTypesMetadata> loginTypes = authentications.stream()
                .map(authentication ->
                        UserLoginTypesMetadata.builder()
                                .username(authentication.getIdentifier())
                                .loginType(authentication.getLoginType())
                                .lastLoginDt(authentication.getLastLoginDt())
                                .build()
                ).collect(Collectors.toList());
        return GetUserResponseDto.builder()
                .id("u_".concat(String.valueOf(user.getId())))
                .username(user.getUsername())
                .status(user.getStatus())
                .loginTypes(loginTypes)
                .metadata(user.getMetadata())
                .createDt(user.getCreateDt())
                .updateDt(user.getUpdateDt())
                .sourceSystemTags(user.getSourceSystemTags())
                .isActive(user.getIsActive())
                .build();
    }


    public static PaginationDto.PaginationDtoBuilder getListWithPaginationResponseDto(List<?> data, Page<?> page) {
        return PaginationDto.builder()
                .data(data)
                .pagination(Pagination.create(page));
    }

    public static GetAuthenticationLogResponseDto getAuthenticationLogResponseDto(AuthenticationLog log) {
        return GetAuthenticationLogResponseDto.builder()
                .id("al_".concat(String.valueOf(log.getId())))
                .type(log.getType())
                .logScope(log.getLogScope())
                .system(log.getSystem())
                .domain(log.getDomain())
                .event(log.getEvent())
                .actionBy(log.getActionBy())
                .traceId(log.getTraceId())
                .correlationId(log.getCorrelationId())
                .trafficTimeInMilliseconds(log.getTrafficTimeInMilliseconds())
                .content(log.getContent())
                .requestBody(log.getRequestBody())
                .responseBody(log.getResponseBody())
                .metadata(log.getMetadata())
                .createDt(log.getCreateDt())
                .build();
    }

    public static GetSystemConfigurationRequestDto getSystemConfiguration(SystemConfiguration systemConfiguration) {
        return GetSystemConfigurationRequestDto.builder()
                .id("sc_".concat(String.valueOf(systemConfiguration.getId())))
                .name(systemConfiguration.getName())
                .target(systemConfiguration.getTarget())
                .scope(systemConfiguration.getScope())
                .value(systemConfiguration.getValue())
                .isActive(systemConfiguration.getIsActive())
                .createDt(systemConfiguration.getCreateDt())
                .updateDt(systemConfiguration.getUpdateDt())
                .build();
    }

    public static PendingVerifyUserResponseDto getPendingVerifyUserResponseDto(String username, OtpMetadata otpMetadata) {
        return PendingVerifyUserResponseDto.builder()
                .username(username)
                .code(otpMetadata.getCode())
                .ttl(otpMetadata.getTtl())
                .build();
    }

    public static PendingVerifyUserResponseDto getDefaultPendingVerifyUserResponseDto(String username) {
        return PendingVerifyUserResponseDto.builder()
                .username(username)
                .build();
    }

    public static GetUserRouteResponseDto getGetUserRouteResponseDto(UserRoute userRoute, Object tenantContext) {
        if (tenantContext == null) { // QUICK return
            return GetUserRouteResponseDto.builder()
                    .id(String.valueOf(userRoute.getId()))
                    .userId(String.valueOf(userRoute.getUserId()))
                    .routes(userRoute.getActualRoutes())
                    .build();
        }

        Map tenant = JSONUtil.convertFromObject(tenantContext, Map.class);
        i18n name = JSONUtil.convertFromObject(tenant.get("name"), i18n.class);
        return GetUserRouteResponseDto.builder()
                .id(String.valueOf(userRoute.getId()))
                .userId(String.valueOf(userRoute.getUserId()))
                .tenantId((String) tenant.get("id"))
                .tenantKey((String) tenant.get("key"))
                .tenantName(name)
                .routes(userRoute.getActualRoutes())
                .build();
    }

    public static GetRbacTemplateResponseDto getRbacTemplateResponseDto(RbacTemplate rbacTemplate) {
        return GetRbacTemplateResponseDto.builder()
                .id("rbac_".concat(String.valueOf(rbacTemplate.getId())))
                .name(rbacTemplate.getName())
                .description(rbacTemplate.getDescription())
                .permissions(rbacTemplate.getPermissions())
                .build();
    }

    public static GetUserPermissionResponseDto getUserPermissionResponseDto(UserPermission userPermission) {
        Map<String, Object> actualPermissions = userPermission.getActualPermissions();
        return GetUserPermissionResponseDto.builder()
                .id("upm_".concat(String.valueOf(userPermission.getId())))
                .version(userPermission.getApiVersion())
                .userId("u_".concat(String.valueOf(userPermission.getUserId())))
                .roles(actualPermissions.keySet().stream().toList())
                .permissions(actualPermissions)
                .build();
    }

    public static GetUserProfileResponseDto getUserProfileResponseDto(UserProfile userProfile) {
        return GetUserProfileResponseDto.builder()
                .id("up_".concat(String.valueOf(userProfile.getId())))
                .userId("u_".concat(String.valueOf(userProfile.getUserId())))
                .context(userProfile.getContext())
                .alias(userProfile.getAlias())
                .createDt(userProfile.getCreateDt())
                .updateDt(userProfile.getUpdateDt())
                .build();
    }

    public static GetUserPreferenceResponseDto getGetUserPreferenceResponseDto(UserPreference userPreference, String targetPreference) {
        return GetUserPreferenceResponseDto.builder()
                .updateDt(userPreference.getUpdateDt())
                .context(userPreference.getActualPreference().get(targetPreference))
                .build();
    }

    public static GetUserPreferenceResponseDto getGetUserPreferenceResponseDto(UserPreference userPreference) {
        return GetUserPreferenceResponseDto.builder()
                .updateDt(userPreference.getUpdateDt())
                .context(userPreference.getActualPreference())
                .build();
    }

    public static GetUserDeviceResponseDto getUserDeviceResponseDto(UserDevice userDevice) {
        GetUserDeviceResponseDto responseDto = GetUserDeviceResponseDto.builder()
                .id("ud_".concat(String.valueOf(userDevice.getId())))
                .userId("u_".concat(String.valueOf(userDevice.getUserId())))
                .context(userDevice.getContext())
                .build();
        responseDto.setCreateDt(userDevice.getCreateDt());
        return responseDto;
    }

    public static GetUserVerificationResponseDto getUserVerificationResponseDto(UserVerification userVerification) {
        GetUserVerificationResponseDto responseDto = GetUserVerificationResponseDto.builder()
                .id("uv_".concat(String.valueOf(userVerification.getId())))
                .userId("u_".concat(String.valueOf(userVerification.getUserId())))
                .detail(userVerification.getDetail())
                .status(userVerification.getStatus())
                .build();
        responseDto.setCreateDt(userVerification.getCreateDt());
        responseDto.setUpdateDt(userVerification.getUpdateDt());
        return responseDto;
    }
}
