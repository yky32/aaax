package com.aaax.usecase.user_customization;

import com.aaax.core.entity.dto.FileMetadata;
import com.aaax.core.entity.dto.util.response.GetCdnResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.RetrofitCallHandler;
import com.aaax.entity.enu.UserProfileType;
import com.aaax.entity.po.user.User;
import com.aaax.entity.po.user_management.UserProfile;
import com.aaax.exception.response.UserProfileErrorResponse;
import com.aaax.repository.UserProfileRepository;
import com.aaax.service.UaaService;
import com.aaax.service.UserProfileService;
import com.aaax.usecase.HandleFileUseCase;
import com.aaax.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static com.aaax.entity.dto.Common.S3_PATH_USER_PROFILE;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateAvatarUseCase implements UseCase<Long, List<MultipartFile>> {

    private final UaaService uaaService;
    private final UserProfileRepository userProfileRepository;
    private final HandleFileUseCase handleFileUseCase;

    @Override
    public void execute(Long userId, List<MultipartFile> files) {
        if (files.size() > 1) {
            throw new BizException(UserProfileErrorResponse.UPR0002);
        }
        User user = uaaService.getById(userId);
        UserProfile userProfile = userProfileRepository.findByUserIdAndType(userId, UserProfileType.DEFAULT.name()).orElseThrow(() -> new BizException(UserProfileErrorResponse.UPR0001, Map.of("userId", userId)));
        Map context = JSONUtil.convertFromObject(userProfile.getContext(), Map.class);
        // replace the avatar field, update as new value
        List<FileMetadata> _files = files.stream().map(file -> {
            String cdnPath = S3_PATH_USER_PROFILE.formatted(userId, userProfile.getId());
            return handleFileUseCase.execute(file, cdnPath);
        }).toList();
        context.put("avatar", _files.get(0).getUrl()); // update the avatar field
        userProfile.setContext(context); // update the context
        userProfileRepository.saveAndFlush(userProfile);
    }

    public void executeUrlOnly(Long userId, String avatarUrl) {
        User user = uaaService.getById(userId);
        UserProfile userProfile = userProfileRepository.findByUserIdAndType(userId, UserProfileType.DEFAULT.name()).orElseThrow(() -> new BizException(UserProfileErrorResponse.UPR0001, Map.of("userId", userId)));
        Map context = JSONUtil.convertFromObject(userProfile.getContext(), Map.class);
        context.put("avatar", avatarUrl); // update the avatar field
        userProfile.setContext(context); // update the context
        userProfileRepository.saveAndFlush(userProfile);
    }
}
