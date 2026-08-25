package com.aaax.server.usecase;

import com.aaax.core.response.PaginationDto;
import com.aaax.server.entity.enu.UaaAspect;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.repository.UserRepository;
import com.aaax.server.repository.UserStatisticRepository;
import com.aaax.server.repository.projection.UserInfoProjection;
import com.aaax.server.service.CommonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserStatisticUseCaseTest {

    @Mock private UserStatisticRepository userStatisticRepository;
    @Mock private CommonService commonService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private UserStatisticUseCase userStatisticUseCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userStatisticUseCase, "timezone", "UTC");
    }

    @Test
    @DisplayName("getUsers should wrap repository page")
    void getUsers_shouldWrapPage() {
        Page<UserInfoProjection> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(userStatisticRepository.userInfo(any())).thenReturn(page);

        PaginationDto.PaginationDtoBuilder result = userStatisticUseCase.getUsers(
                PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createDt")));

        assertNotNull(result);
        verify(userStatisticRepository).userInfo(any());
    }

    @Test
    @DisplayName("usersCount should filter by date and aspect USER")
    void usersCount_shouldCountUsers() {
        when(userRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(User.builder().id(1L).build()));
        when(userStatisticRepository.usersCount(anyList(), any(), any(), any(), eq(UaaAspect.USER)))
                .thenReturn(3L);

        long count = userStatisticUseCase.usersCount(null, List.of("ACTIVE"), null, null, UaaAspect.USER);

        assertEquals(3L, count);
        verify(userStatisticRepository).usersCount(eq(List.of(1L)), eq(List.of("ACTIVE")), any(), any(), eq(UaaAspect.USER));
    }
}
