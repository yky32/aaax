package com.aaax.usecase;

import com.aaax.core.common.jsonfield.DeviceMetadata;
import com.aaax.core.constant.enu.DevicePlatform;
import com.aaax.core.entity.dto.uaa.response.GetUserDeviceResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.PaginationDto;
import com.aaax.core.utils.IdSplitter;
import com.aaax.core.utils.InstantUtil;
import com.aaax.core.utils.ResourcesUtil;
import com.aaax.core.utils.UserAgentUtil;
import com.aaax.entity.dto.request.RegisterUserDeviceRequestDto;
import com.aaax.entity.enu.DeviceResourceType;
import com.aaax.entity.po.user_management.UserDevice;
import com.aaax.exception.response.UserDeviceErrorResponse;
import com.aaax.repository.UserDeviceRepository;
import com.aaax.service.DtoWrapper;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserDeviceUseCase {

    private final UserDeviceRepository userDeviceRepository;
    private final ResourceLoader resourceLoader;
    private final HttpServletRequest request;
    @Value("${config.microservice.timezone:UTC}")
    private String timezone;
    @Value("${config.device-binding.mode}")
    private String deviceBindingMode;

    public GetUserDeviceResponseDto doDeviceBinding(String userId, @Valid RegisterUserDeviceRequestDto dto) {
        Optional<UserDevice> isExistedUserDevice = userDeviceRepository.findByResourceIdAndResourceTypeAndUserId(dto.getSourceSystem(), DeviceResourceType.SYSTEM.name(), Long.valueOf(IdSplitter.split(userId)));
        return null;
    }

    @Transactional
    public GetUserDeviceResponseDto register(String userId, RegisterUserDeviceRequestDto dto) {
        this.validations(dto);
        this.autoFill(dto);

        Optional<UserDevice> isExistedUserDevice = userDeviceRepository.findByResourceIdAndResourceTypeAndUserId(dto.getSourceSystem(), DeviceResourceType.SYSTEM.name(), Long.valueOf(IdSplitter.split(userId)));
        if (isExistedUserDevice.isPresent()) {
            switch (deviceBindingMode) {
                case ("TRUST_LATEST") -> {
                    isExistedUserDevice.get().setContext(List.of(dto.getDevice()));
                    Map control = isExistedUserDevice.get().getControl();
                    Set<String> boundDevices = new HashSet<>((Collection) control.get("boundDevices"));
                    boundDevices.add(dto.getDevice().getProfile().getId());
                    control.put("boundDevices", boundDevices);
                    isExistedUserDevice.get().setControl(control);
                }
                default ->  {
                    List<DeviceMetadata> userDevices = isExistedUserDevice.get().getContext();
                    List<String> existedDeviceIds = userDevices.stream().map(ud -> ud.getProfile().getId()).toList();
                    Map control = isExistedUserDevice.get().getControl();
                    if (existedDeviceIds.contains(dto.getDevice().getProfile().getId())) {
                        for (int i = 0; i < userDevices.size(); i++) {
                            // replace to find out
                            DeviceMetadata device = userDevices.get(i);
                            // Check if the device IDs are equal (case insensitive)
                            if (device.getProfile().getId().equalsIgnoreCase(dto.getDevice().getProfile().getId())) {
                                userDevices.set(i, dto.getDevice()); // Replace the existing device with the new device

                                // set latest
                                Set<String> boundDevices = new HashSet<>((Collection) control.get("boundDevices"));
                                boundDevices.add(dto.getDevice().getProfile().getId());
                                control.put("boundDevices", boundDevices);
                                isExistedUserDevice.get().setControl(control);
                                break; // stopped.
                            }
                        }
                    } else {
                        // new device
                        userDevices.add(dto.getDevice());
                    }
                }
            }
            userDeviceRepository.save(isExistedUserDevice.get()); // AUTO detect isEdited with JPA
            return DtoWrapper.getUserDeviceResponseDto(isExistedUserDevice.get());
        }

        Map control = this.getUserDeviceConfig();
        List<DeviceMetadata> devices = new ArrayList<>();
        devices.add(dto.getDevice());
        UserDevice userDevice = UserDevice.builder()
                .userId(Long.valueOf(IdSplitter.split(userId)))
                .resourceId(dto.getSourceSystem())
                .resourceType(DeviceResourceType.SYSTEM.name())
                .context(devices)
                .control(control)
                .build();
        userDevice = userDeviceRepository.save(userDevice);
        return DtoWrapper.getUserDeviceResponseDto(userDevice);
    }

    private Map getUserDeviceConfig() {
        return ResourcesUtil.readJson("config/user_device/user_devices_control.json", resourceLoader, Map.class);
    }

    private void autoFill(RegisterUserDeviceRequestDto dto) {
        if (Optional.ofNullable(dto.getDevice().getProfile().getPlatform()).isPresent()) {
            return;
        }
        String userAgentString = request.getHeader("User-Agent");
        DevicePlatform devicePlatform = UserAgentUtil.detectOperatingSystem(userAgentString);
        dto.getDevice().getProfile().setPlatform(devicePlatform);
    }

    private void validations(RegisterUserDeviceRequestDto dto) {
        if (Optional.ofNullable(dto.getDevice()).isEmpty()) {
            throw new BizException(UserDeviceErrorResponse.UDV0002, "Plz provide [%s] .".formatted("device"));
        }

        if (Optional.ofNullable(dto.getDevice().getProfile()).isEmpty()) {
            throw new BizException(UserDeviceErrorResponse.UDV0002, "Plz provide [%s] .".formatted("device.profile"));
        } else {
            if (Optional.ofNullable(dto.getDevice().getProfile().getId()).isEmpty()) {
                throw new BizException(UserDeviceErrorResponse.UDV0002, "Plz provide [%s] .".formatted("device.profile.id"));
            }
            if (Optional.ofNullable(dto.getDevice().getProfile().getDisplayName()).isEmpty()) {
                throw new BizException(UserDeviceErrorResponse.UDV0002, "Plz provide [%s] .".formatted("device.profile.displayName"));
            }
        }

        Map config = ResourcesUtil.readJson("config/user_device/user_devices_validations.json", resourceLoader, Map.class);
        if (Optional.ofNullable(dto.getDevice().getToken()).isPresent()) {
            List<String> validOptions = (List<String>) config.get("device.token");
            boolean isValid = dto.getDevice().getToken().keySet().stream().anyMatch(validOptions::contains);
            if (!isValid) {
                String message = "%s .".formatted(dto.getDevice().getToken().keySet());
                throw new BizException(UserDeviceErrorResponse.UDV0002, Map.of("validOptions", validOptions, "message", message));
            }
        }
    }

    public GetUserDeviceResponseDto myDevicesOfSourceSystem(String userId, String sourceSystem) {
        UserDevice userDevice = userDeviceRepository.findByResourceIdAndResourceTypeAndUserId(sourceSystem, DeviceResourceType.SYSTEM.name(), Long.valueOf(IdSplitter.split(userId)))
                .orElseGet(() -> this.generateEmptyDevice(userId, sourceSystem));
        return DtoWrapper.getUserDeviceResponseDto(userDevice);
    }

    private UserDevice generateEmptyDevice(String userId, String sourceSystem) {
        Map control = this.getUserDeviceConfig();
        List<DeviceMetadata> devices = new ArrayList<>();
        UserDevice userDevice = UserDevice.builder()
                .userId(Long.valueOf(IdSplitter.split(userId)))
                .resourceId(sourceSystem)
                .resourceType(DeviceResourceType.SYSTEM.name())
                .context(devices)
                .control(control)
                .build();
        return userDeviceRepository.save(userDevice);
    }

    public List<GetUserDeviceResponseDto> myDevices(String userId, String sourceSystem) {
        List<UserDevice> userDevices = userDeviceRepository.findAllByResourceIdAndResourceTypeAndUserId(sourceSystem, DeviceResourceType.SYSTEM.name(), Long.valueOf(IdSplitter.split(userId)));
        return userDevices.stream()
                .map(DtoWrapper::getUserDeviceResponseDto)
                .toList();
    }

    public PaginationDto.PaginationDtoBuilder getAll(Pageable pageable, String startDt, String endDt) {
        Instant _startDt = StringUtils.isBlank(startDt) ? InstantUtil.parse(InstantUtil.EARLIEST_DATE) : InstantUtil.parse_tz(startDt, timezone);
        Instant _endDt = StringUtils.isBlank(endDt) ? InstantUtil.parse(InstantUtil.NEVER_EXPIRED) : InstantUtil.parse_tz(endDt, timezone);
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber() - 1, pageable.getPageSize(),
                pageable.getSort().iterator().next().getDirection(),
                pageable.getSort().iterator().next().getProperty()
        );
        Specification<UserDevice> specification = Specification.where(null);
        specification = specification.and(((root, query, builder) -> builder.between(root.get("createDt"), _startDt, _endDt.plusSeconds(1)))); // in-case last second. of 23:59:59
        Page<UserDevice> userDevices = userDeviceRepository.findAll(specification, pageRequest);
        log.info("-- ================ Fetch ALL userDevices =======================");
        List<GetUserDeviceResponseDto> data = userDevices.getContent().stream()
                .map(DtoWrapper::getUserDeviceResponseDto)
                .collect(Collectors.toList());
        return DtoWrapper.getListWithPaginationResponseDto(data, userDevices);
    }

    public GetUserDeviceResponseDto getOne(String id) {
        UserDevice userDevice = userDeviceRepository.findById(Long.valueOf(IdSplitter.split(id))).orElseThrow(() -> new BizException(UserDeviceErrorResponse.UDV0001, Map.of("id", id)));
        return DtoWrapper.getUserDeviceResponseDto(userDevice);
    }
}
