package com.aaax.server.usecase.user_customization;

import com.aaax.core.entity.dto.FileMetadata;
import com.aaax.core.exception.BizException;
import com.aaax.server.entity.enu.UserProfileType;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.entity.po.user_management.UserProfile;
import com.aaax.server.repository.UserProfileRepository;
import com.aaax.server.service.AaaxService;
import com.aaax.server.usecase.HandleFileUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateAvatarUseCaseTest {

    @Mock private AaaxService aaaxService;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private HandleFileUseCase handleFileUseCase;
    @Mock private MultipartFile file;

    @InjectMocks
    private UpdateAvatarUseCase updateAvatarUseCase;

    @Test
    @DisplayName("execute should reject more than one file")
    void execute_shouldRejectMultipleFiles() {
        assertThrows(BizException.class, () -> updateAvatarUseCase.execute(1L, List.of(file, file)));
    }

    @Test
    @DisplayName("execute should upload and persist avatar url")
    void execute_shouldPersistAvatar() {
        when(aaaxService.getById(1L)).thenReturn(User.builder().id(1L).build());
        UserProfile profile = UserProfile.builder()
                .id(9L).userId(1L)
                .context(new HashMap<>(Map.of("avatar", "old")))
                .build();
        when(userProfileRepository.findByUserIdAndType(1L, UserProfileType.DEFAULT.name()))
                .thenReturn(Optional.of(profile));
        when(handleFileUseCase.execute(eq(file), anyString()))
                .thenReturn(FileMetadata.builder().url("https://cdn/a.png").build());

        updateAvatarUseCase.execute(1L, List.of(file));

        verify(userProfileRepository).saveAndFlush(argThat(p ->
                "https://cdn/a.png".equals(((Map<?, ?>) p.getContext()).get("avatar"))));
    }

    @Test
    @DisplayName("executeUrlOnly should update avatar without upload")
    void executeUrlOnly_shouldUpdate() {
        when(aaaxService.getById(2L)).thenReturn(User.builder().id(2L).build());
        UserProfile profile = UserProfile.builder()
                .id(3L).userId(2L)
                .context(new HashMap<>(Map.of("avatar", "old")))
                .build();
        when(userProfileRepository.findByUserIdAndType(2L, UserProfileType.DEFAULT.name()))
                .thenReturn(Optional.of(profile));

        updateAvatarUseCase.executeUrlOnly(2L, "https://cdn/new.png");

        assertEquals("https://cdn/new.png", ((Map<?, ?>) profile.getContext()).get("avatar"));
        verify(userProfileRepository).saveAndFlush(profile);
    }

    @Test
    @DisplayName("execute should throw when profile missing")
    void execute_shouldThrowWhenProfileMissing() {
        when(aaaxService.getById(1L)).thenReturn(User.builder().id(1L).build());
        when(userProfileRepository.findByUserIdAndType(1L, UserProfileType.DEFAULT.name()))
                .thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> updateAvatarUseCase.execute(1L, List.of(file)));
    }
}
