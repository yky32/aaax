package com.aaax.server.usecase;

import com.aaax.core.api.UtilApiClient;
import com.aaax.core.constant.RegexPatternConstant;
import com.aaax.core.entity.dto.uaa.response.GetUserProfileResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserVerificationResponseDto;
import com.aaax.core.entity.dto.util.response.GetCdnResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.utils.*;
import com.aaax.server.entity.dto.json_context.user_management.UserProfileMetadata;
import com.aaax.server.entity.dto.request.UpdateUserProfileRequestDto;
import com.aaax.server.entity.enu.UaaAspect;
import com.aaax.server.entity.enu.UserProfileType;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.entity.po.user_management.UserProfile;
import com.aaax.server.exception.response.UaaErrorResponse;
import com.aaax.server.exception.response.UserProfileErrorResponse;
import com.aaax.server.repository.UserProfileRepository;
import com.aaax.server.repository.UserRepository;
import com.aaax.server.service.DtoWrapper;
import com.aaax.server.service.UaaService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserProfileUseCase {
    private static final String S3_PATH = "user-profiles/";
    private final UserProfileRepository userProfileRepository;
    private final UtilApiClient utilApiClient;
    private final ResourceLoader resourceLoader;
    private final UaaService uaaService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    @Lazy
    private UserIdentityVerificationUseCase userIdentityVerificationUseCase;

    public GetUserProfileResponseDto getUserProfile(String userId, List<String> aspects) {
        GetUserProfileResponseDto userProfile = getUserProfile(userId);
        for (String aspect : aspects) {
            //userProfile.setContext(this.attachAdditionalAspects(aspect, userId, userProfile.getContext()));
        }
        return userProfile;
    }

    private Object attachAdditionalAspects(String aspect, String userId, Object context) {
        switch (aspect.toUpperCase()) { // upper-case for ignore-case
            case (UaaAspect.VERIFICATION) -> {
                List<GetUserVerificationResponseDto> userVerifications = userIdentityVerificationUseCase.myVerifications(userId);
                Map _context = JSONUtil.convertFromObject(context, Map.class);
                Map _verification = (Map) _context.getOrDefault(aspect.toLowerCase(), new HashMap<>());
                _verification.put("idvCallbacks",
                        userVerifications.stream().map(uv -> {
                            Map detail = JSONUtil.convertFromObject(uv.getDetail(), Map.class);
                            return detail.getOrDefault("idvCallback", "NA");
                        }).toList()
                ); // FETCH related records
                _context.put(aspect.toLowerCase(), _verification);
                context = _context;
            }
            default ->
                    throw new BizException(UaaErrorResponse.UAA4400, "Invalid [a] parameters %s from %s".formatted(aspect, UaaAspect.USER_PROFILES));
        }
        return context;
    }

    public GetUserProfileResponseDto getUserProfile(String userId) {
        UserProfile userProfile = userProfileRepository.findByUserIdAndType(Long.valueOf(IdSplitter.split(userId)), UserProfileType.DEFAULT.name()).orElseGet(
                () -> {
                    String identifier;
                    try {
                        identifier = (String) ((Map<?, ?>) JwtUtil.getFromJwt(JwtUtil.METADATA)).get("identifier");
                    } catch (Exception e) {
                        GetUserResponseDto userResponseDto = uaaService.getOne(userId);
                        identifier = userResponseDto.getUsername();
                    }
                    return doCreateDefault(new HashMap(), Long.valueOf(IdSplitter.split(userId)));
                });
        return DtoWrapper.getUserProfileResponseDto(userProfile);
    }

    public GetUserProfileResponseDto getOneProfile(String alias) {
        UserProfile userProfile = userProfileRepository.findByAlias(alias).orElseThrow(() -> new BizException(UserProfileErrorResponse.UPR0001, Map.of("alias", alias)));
        return DtoWrapper.getUserProfileResponseDto(userProfile);
    }

    public GetUserProfileResponseDto updateUserProfileMgt(String userId, UpdateUserProfileRequestDto requestDto, String systemSource) {
        User user = uaaService.getById(userId);
        return updateUserProfile(String.valueOf(user.getId()), requestDto, user.getUsername(), systemSource);
    }

    @SneakyThrows
    @Transactional
    public GetUserProfileResponseDto updateUserProfile(String userId, UpdateUserProfileRequestDto requestDto, String identifier, String systemSource) {
        UserProfile userProfile = userProfileRepository.findByUserIdAndType(Long.valueOf(IdSplitter.split(userId)), UserProfileType.DEFAULT.name()).orElseGet(() -> generateUserProfile(identifier, Long.valueOf(userId)));

        if (ObjectUtils.isNotEmpty(requestDto.getIcon())) {
            RequestBody requestBody = RequestBody.create(MediaType.parse(Objects.requireNonNull(requestDto.getIcon().getContentType())), requestDto.getIcon().getBytes());
            List<GetCdnResponseDto> cdnFiles = RetrofitCallHandler._execute(utilApiClient.upload(
                    S3_PATH.concat(userId).concat("/"),
                    MultipartBody.Part.createFormData("files", requestDto.getIcon().getOriginalFilename(), requestBody)
            )).getData();
            requestDto.getContext().put("avatar", Optional.ofNullable(cdnFiles.get(0)).isEmpty() ? null : cdnFiles.get(0).getLink().getUrl());
        }
        if (Optional.ofNullable(requestDto.getType()).isPresent()) {
            userProfile.setType(requestDto.getType());
        }
        Map originalProfile = JSONUtil.convertFromObject(userProfile.getContext(), Map.class);
        Map originalVerification = (Map) originalProfile.getOrDefault("verification", new HashMap<>());
        String originalIdvStatus = (String) originalVerification.getOrDefault("idvStatus", "NA");
        userProfile.setContext(DtoUtil.nestedUpdateBuilderToMap(requestDto.getContext(), userProfile.getContext()));
        userProfile = userProfileRepository.save(userProfile);
        Map verification = (Map) requestDto.getContext().getOrDefault("verification", new HashMap<>());
        String idvStatus = (String) verification.getOrDefault("idvStatus", "NA");
        // only trigger when idv status is changed and not equal to NA
        if (!idvStatus.equalsIgnoreCase("NA") && !originalIdvStatus.equals(idvStatus)) {
            userIdentityVerificationUseCase.afterVerification(userId, idvStatus, systemSource);
        }

        return DtoWrapper.getUserProfileResponseDto(userProfile);
    }

    /**
     * System-generated Profile based on JWT
     *
     * @return - PO
     */
    @Transactional
    private UserProfile generateUserProfile(String identifier, Long userId) {
        return doCreateDefault(new HashMap<>(), userId);
    }

    /**
     * @param metadata - pass from client side
     * @param userId   - identifier
     * @return
     */
    public UserProfile doCreateDefault(Map metadata, Long userId) {
        Optional<UserProfile> isExistedUserProfile = userProfileRepository.findByUserIdAndType(Long.valueOf(IdSplitter.split(userId)), UserProfileType.DEFAULT.name());
        if (isExistedUserProfile.isPresent()) {
            return isExistedUserProfile.get();
        }
        User user = uaaService.getById(Long.valueOf(IdSplitter.split(userId)));
        Map defaulProfileMetadata = this._defaultMetadataJson();
        defaulProfileMetadata.put("email", ValidationUtil.patternMatches(user.getUsername(), RegexPatternConstant.EMAIL_PATTERN) ? user.getUsername() : "INVALID EMAIL");
        if (Optional.ofNullable(metadata).isEmpty()) {
            metadata = new HashMap();
        }
        defaulProfileMetadata.forEach(metadata::putIfAbsent);
        metadata.putIfAbsent("verification", ResourcesUtil.readJson("config/user_profile/user_profile_verification.json", resourceLoader, Map.class));
        metadata.putIfAbsent("usage", ResourcesUtil.readJson("config/user_profile/user_profile_usage.json", resourceLoader, Map.class));

        UserProfile userProfile = UserProfile.builder()
                .userId(userId)
                .type(UserProfileType.DEFAULT.name())
                .alias(user.getUsername())
                .context(metadata)
                .build();
        return userProfileRepository.save(userProfile);
    }

    public Map _defaultMetadataJson() {
        Map metadata = JSONUtil.convertFromObject(UserProfileMetadata.builder().build(), Map.class);
        metadata.put("avatar", null);
        return metadata;
    }
}
