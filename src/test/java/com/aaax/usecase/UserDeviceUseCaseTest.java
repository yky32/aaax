package com.aaax.usecase;

import com.aaax.core.common.jsonfield.DeviceMetadata;
import com.aaax.core.common.jsonfield.DeviceProfileMetadata;
import com.aaax.core.entity.dto.uaa.response.GetUserDeviceResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.utils.ResourcesUtil;
import com.aaax.entity.dto.request.RegisterUserDeviceRequestDto;
import com.aaax.entity.enu.DeviceResourceType;
import com.aaax.entity.po.user_management.UserDevice;
import com.aaax.repository.UserDeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDeviceUseCaseTest {

    @Mock private UserDeviceRepository userDeviceRepository;
    @Mock private ResourceLoader resourceLoader;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private UserDeviceUseCase userDeviceUseCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userDeviceUseCase, "timezone", "UTC");
        ReflectionTestUtils.setField(userDeviceUseCase, "deviceBindingMode", "DEFAULT");
    }

    private DeviceMetadata device(String id) {
        return DeviceMetadata.builder()
                .profile(DeviceProfileMetadata.builder().id(id).displayName("Phone").build())
                .token(Map.of("fcm", "t"))
                .build();
    }

    @Test
    @DisplayName("register should create new device when none exists")
    void register_shouldCreateNew() {
        RegisterUserDeviceRequestDto dto = RegisterUserDeviceRequestDto.builder()
                .sourceSystem("APP")
                .device(device("d1"))
                .build();
        when(userDeviceRepository.findByResourceIdAndResourceTypeAndUserId("APP", DeviceResourceType.SYSTEM.name(), 1L))
                .thenReturn(Optional.empty());
        when(userDeviceRepository.save(any())).thenAnswer(inv -> {
            UserDevice ud = inv.getArgument(0);
            ud.setId(10L);
            return ud;
        });
        try (MockedStatic<ResourcesUtil> resources = mockStatic(ResourcesUtil.class)) {
            resources.when(() -> ResourcesUtil.readJson(contains("validations"), any(), eq(Map.class)))
                    .thenReturn(Map.of("device.token", List.of("fcm")));
            resources.when(() -> ResourcesUtil.readJson(contains("control"), any(), eq(Map.class)))
                    .thenReturn(Map.of("boundDevices", new HashSet<>()));

            GetUserDeviceResponseDto result = userDeviceUseCase.register("u_1", dto);
            assertEquals("ud_10", result.getId());
        }
    }

    @Test
    @DisplayName("register should reject missing device")
    void register_shouldRejectMissingDevice() {
        assertThrows(BizException.class, () -> userDeviceUseCase.register("u_1",
                RegisterUserDeviceRequestDto.builder().sourceSystem("APP").build()));
    }

    @Test
    @DisplayName("myDevicesOfSourceSystem should return existing device")
    void myDevicesOfSourceSystem_shouldReturnExisting() {
        UserDevice device = UserDevice.builder().id(5L).userId(1L).context(List.of()).build();
        when(userDeviceRepository.findByResourceIdAndResourceTypeAndUserId("APP", DeviceResourceType.SYSTEM.name(), 1L))
                .thenReturn(Optional.of(device));

        GetUserDeviceResponseDto result = userDeviceUseCase.myDevicesOfSourceSystem("u_1", "APP");
        assertEquals("ud_5", result.getId());
    }

    @Test
    @DisplayName("myDevicesOfSourceSystem should generate empty device when missing")
    void myDevicesOfSourceSystem_shouldGenerateEmpty() {
        when(userDeviceRepository.findByResourceIdAndResourceTypeAndUserId("APP", DeviceResourceType.SYSTEM.name(), 1L))
                .thenReturn(Optional.empty());
        when(userDeviceRepository.save(any())).thenAnswer(inv -> {
            UserDevice ud = inv.getArgument(0);
            ud.setId(8L);
            return ud;
        });
        try (MockedStatic<ResourcesUtil> resources = mockStatic(ResourcesUtil.class)) {
            resources.when(() -> ResourcesUtil.readJson(anyString(), any(), eq(Map.class)))
                    .thenReturn(Map.of("boundDevices", new HashSet<>()));

            GetUserDeviceResponseDto result = userDeviceUseCase.myDevicesOfSourceSystem("u_1", "APP");
            assertEquals("ud_8", result.getId());
        }
    }

    @Test
    @DisplayName("myDevices should map list")
    void myDevices_shouldMapList() {
        when(userDeviceRepository.findAllByResourceIdAndResourceTypeAndUserId("APP", DeviceResourceType.SYSTEM.name(), 1L))
                .thenReturn(List.of(UserDevice.builder().id(1L).userId(1L).context(List.of()).build()));
        assertEquals(1, userDeviceUseCase.myDevices("u_1", "APP").size());
    }

    @Test
    @DisplayName("getOne should return device or throw")
    void getOne_shouldReturnOrThrow() {
        when(userDeviceRepository.findById(3L)).thenReturn(Optional.of(
                UserDevice.builder().id(3L).userId(1L).context(List.of()).build()));
        assertEquals("ud_3", userDeviceUseCase.getOne("ud_3").getId());

        when(userDeviceRepository.findById(4L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> userDeviceUseCase.getOne("ud_4"));
    }

    @Test
    @DisplayName("doDeviceBinding should return null placeholder")
    void doDeviceBinding_shouldReturnNull() {
        when(userDeviceRepository.findByResourceIdAndResourceTypeAndUserId(anyString(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        assertNull(userDeviceUseCase.doDeviceBinding("u_1",
                RegisterUserDeviceRequestDto.builder().sourceSystem("APP").build()));
    }
}
