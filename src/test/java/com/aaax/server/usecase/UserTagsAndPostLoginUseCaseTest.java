package com.aaax.server.usecase;

import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserTagsAndPostLoginUseCaseTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private UserTagsUseCase userTagsUseCase;

    @InjectMocks
    private PostLoginUseCase postLoginUseCase;

    @Test
    @DisplayName("UserTagsUseCase.assign should add source system tag")
    void userTags_assign_shouldAddTag() {
        User user = User.builder().id(1L).username("u@test.com")
                .authentications(List.of())
                .sourceSystemTags(new ArrayList<>(List.of("QS")))
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GetUserResponseDto result = userTagsUseCase.assign("1", "WEB");
        assertEquals("u_1", result.getId());
        assertTrue(result.getSourceSystemTags().contains("WEB"));
    }

    @Test
    @DisplayName("UserTagsUseCase.assign should throw when user missing")
    void userTags_assign_shouldThrowWhenMissing() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> userTagsUseCase.assign("9", "QS"));
    }

    @Test
    @DisplayName("PostLoginUseCase.assign should add source system tag")
    void postLogin_assign_shouldAddTag() {
        User user = User.builder().id(2L).username("u@test.com")
                .authentications(List.of())
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GetUserResponseDto result = postLoginUseCase.assign("2", "IOS");
        assertTrue(result.getSourceSystemTags().contains("IOS"));
    }
}
