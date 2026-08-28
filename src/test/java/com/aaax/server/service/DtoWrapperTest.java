package com.aaax.server.service;

import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.constant.enu.UserVerificationStatus;
import com.aaax.core.entity.dto.aaax.response.GetUserDeviceResponseDto;
import com.aaax.core.entity.dto.aaax.response.GetUserPreferenceResponseDto;
import com.aaax.core.entity.dto.aaax.response.GetUserProfileResponseDto;
import com.aaax.core.entity.dto.aaax.response.GetUserResponseDto;
import com.aaax.core.entity.dto.aaax.response.GetUserVerificationResponseDto;
import com.aaax.core.response.PaginationDto;
import com.aaax.server.entity.dto.json_context.OtpMetadata;
import com.aaax.server.entity.dto.response.GetRbacTemplateResponseDto;
import com.aaax.server.entity.dto.response.GetSystemConfigurationRequestDto;
import com.aaax.server.entity.dto.response.GetUserPermissionResponseDto;
import com.aaax.server.entity.dto.response.GetUserRouteResponseDto;
import com.aaax.server.entity.dto.response.PendingVerifyUserResponseDto;
import com.aaax.server.entity.po.UserRoute;
import com.aaax.server.entity.po.configuration.SystemConfiguration;
import com.aaax.server.entity.po.rbac.RbacTemplate;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.entity.po.user_management.UserDevice;
import com.aaax.server.entity.po.user_management.UserPermission;
import com.aaax.server.entity.po.user_management.UserPreference;
import com.aaax.server.entity.po.user_management.UserProfile;
import com.aaax.server.entity.po.user_verification.UserVerification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DtoWrapperTest {

    @Test
    @DisplayName("getUserResponseDto should map user and login types")
    void getUserResponseDto_shouldMapUser() {
        User user = User.builder()
                .id(10L)
                .username("user@test.com")
                .status(UserStatus.ACTIVE)
                .sourceSystemTags(List.of("app"))
                .build();
        user.setIsActive(true);
        user.setCreateDt(Instant.parse("2024-01-01T00:00:00Z"));
        user.setUpdateDt(Instant.parse("2024-01-02T00:00:00Z"));
        Authentication auth = Authentication.builder()
                .identifier("user@test.com")
                .loginType(LoginType.EMAIL)
                .lastLoginDt(Instant.parse("2024-01-03T00:00:00Z"))
                .build();

        GetUserResponseDto dto = DtoWrapper.getUserResponseDto(user, List.of(auth));

        assertEquals("u_10", dto.getId());
        assertEquals("user@test.com", dto.getUsername());
        assertEquals(1, dto.getLoginTypes().size());
        assertEquals(List.of("app"), dto.getSourceSystemTags());
        assertEquals(true, dto.getIsActive());
    }

    @Test
    @DisplayName("getListWithPaginationResponseDto should wrap page data")
    void getListWithPaginationResponseDto_shouldWrap() {
        Page<String> page = new PageImpl<>(List.of("a"), PageRequest.of(0, 10), 1);

        PaginationDto.PaginationDtoBuilder builder = DtoWrapper.getListWithPaginationResponseDto(List.of("a"), page);

        assertNotNull(builder.build());
        assertEquals(List.of("a"), builder.build().getData());
    }

    @Test
    @DisplayName("getSystemConfiguration should map configuration fields")
    void getSystemConfiguration_shouldMap() {
        SystemConfiguration config = SystemConfiguration.builder()
                .id(5L)
                .name("NAME")
                .target("GLOBAL")
                .scope("SCOPE")
                .value(List.of("x"))
                .build();
        config.setCreateDt(Instant.now());

        GetSystemConfigurationRequestDto dto = DtoWrapper.getSystemConfiguration(config);

        assertEquals("sc_5", dto.getId());
        assertEquals("NAME", dto.getName());
        assertEquals(List.of("x"), dto.getValue());
    }

    @Test
    @DisplayName("pending verify DTOs should map otp metadata")
    void pendingVerifyDtos_shouldMap() {
        OtpMetadata metadata = OtpMetadata.builder().code("123456").ttl(60).build();

        PendingVerifyUserResponseDto withOtp = DtoWrapper.getPendingVerifyUserResponseDto("user@test.com", metadata);
        PendingVerifyUserResponseDto defaults = DtoWrapper.getDefaultPendingVerifyUserResponseDto("user@test.com");

        assertEquals("123456", withOtp.getCode());
        assertEquals(60, withOtp.getTtl());
        assertEquals("user@test.com", defaults.getUsername());
        assertNull(defaults.getCode());
    }

    @Test
    @DisplayName("getGetUserRouteResponseDto should handle null and populated tenant context")
    void getUserRouteResponseDto_shouldHandleTenantContext() {
        UserRoute route = UserRoute.builder()
                .id(1L)
                .userId(2L)
                .actualRoutes(Map.of("path", "/home"))
                .build();

        GetUserRouteResponseDto withoutTenant = DtoWrapper.getGetUserRouteResponseDto(route, null);
        assertEquals("1", withoutTenant.getId());
        assertNull(withoutTenant.getTenantId());

        Map<String, Object> tenant = Map.of(
                "id", "t_1",
                "key", "tenant-key",
                "name", Map.of("en", "Tenant", "zh", "租户")
        );
        GetUserRouteResponseDto withTenant = DtoWrapper.getGetUserRouteResponseDto(route, tenant);
        assertEquals("t_1", withTenant.getTenantId());
        assertEquals("tenant-key", withTenant.getTenantKey());
        assertNotNull(withTenant.getTenantName());
    }

    @Test
    @DisplayName("getRbacTemplateResponseDto should map template")
    void getRbacTemplateResponseDto_shouldMap() {
        RbacTemplate template = RbacTemplate.builder()
                .id(3L)
                .name("admin")
                .description("desc")
                .permissions(Map.of("role", Map.of()))
                .build();

        GetRbacTemplateResponseDto dto = DtoWrapper.getRbacTemplateResponseDto(template);

        assertEquals("rbac_3", dto.getId());
        assertEquals("admin", dto.getName());
    }

    @Test
    @DisplayName("getUserPermissionResponseDto should map roles and permissions")
    void getUserPermissionResponseDto_shouldMap() {
        UserPermission permission = UserPermission.builder()
                .id(7L)
                .userId(8L)
                .apiVersion("1.0")
                .actualPermissions(Map.of("normal", Map.of("feature", "ALLOW")))
                .build();

        GetUserPermissionResponseDto dto = DtoWrapper.getUserPermissionResponseDto(permission);

        assertEquals("upm_7", dto.getId());
        assertEquals("u_8", dto.getUserId());
        assertTrue(dto.getRoles().contains("normal"));
    }

    @Test
    @DisplayName("getUserProfileResponseDto should map profile")
    void getUserProfileResponseDto_shouldMap() {
        UserProfile profile = UserProfile.builder()
                .id(1L)
                .userId(2L)
                .alias("nick")
                .context(Map.of("k", "v"))
                .build();
        profile.setCreateDt(Instant.now());
        profile.setUpdateDt(Instant.now());

        GetUserProfileResponseDto dto = DtoWrapper.getUserProfileResponseDto(profile);

        assertEquals("up_1", dto.getId());
        assertEquals("nick", dto.getAlias());
    }

    @Test
    @DisplayName("preference DTOs should map full and targeted preference")
    void preferenceDtos_shouldMap() {
        UserPreference preference = UserPreference.builder()
                .actualPreference(Map.of("theme", "dark", "lang", "en"))
                .build();
        preference.setUpdateDt(Instant.now());

        GetUserPreferenceResponseDto full = DtoWrapper.getGetUserPreferenceResponseDto(preference);
        GetUserPreferenceResponseDto targeted = DtoWrapper.getGetUserPreferenceResponseDto(preference, "theme");

        assertEquals(Map.of("theme", "dark", "lang", "en"), full.getContext());
        assertEquals("dark", targeted.getContext());
    }

    @Test
    @DisplayName("device and verification DTOs should map fields")
    void deviceAndVerification_shouldMap() {
        UserDevice device = UserDevice.builder()
                .id(1L)
                .userId(2L)
                .context(List.of(com.aaax.core.common.jsonfield.DeviceMetadata.builder()
                        .deviceKey("ios-1")
                        .build()))
                .build();
        device.setCreateDt(Instant.now());
        GetUserDeviceResponseDto deviceDto = DtoWrapper.getUserDeviceResponseDto(device);
        assertEquals("ud_1", deviceDto.getId());

        UserVerification verification = UserVerification.builder()
                .id(3L)
                .userId(4L)
                .detail(Map.of("type", "ID"))
                .status(UserVerificationStatus.VERIFIED)
                .build();
        verification.setCreateDt(Instant.now());
        verification.setUpdateDt(Instant.now());
        GetUserVerificationResponseDto verificationDto = DtoWrapper.getUserVerificationResponseDto(verification);
        assertEquals("uv_3", verificationDto.getId());
        assertEquals(UserVerificationStatus.VERIFIED, verificationDto.getStatus());
    }
}
