package com.aaax.server.service;

import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.entity.dto.aaax.response.GetUserMetricsResponseDto;
import com.aaax.core.entity.dto.aaax.response.GetUserPreferenceResponseDto;
import com.aaax.core.entity.dto.aaax.response.GetUserProfileResponseDto;
import com.aaax.core.entity.dto.aaax.response.GetUserResponseDto;
import com.aaax.core.entity.dto.aaax.response.GetUserVerificationResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.server.entity.enu.AaaxAspect;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.repository.AuthenticationRepository;
import com.aaax.server.repository.UserRepository;
import com.aaax.server.repository.UserRouteRepository;
import com.aaax.server.usecase.UserMetricsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AaaxServiceTest {

    @Mock
    private UserRouteRepository userRouteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationRepository authenticationRepository;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private ResourceLoader resourceLoader;
    @Mock
    private CommonService commonService;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private UserMetricsUseCase userMetricsUseCase;

    @InjectMocks
    private AaaxService aaaxService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aaaxService, "userMetricsUseCase", userMetricsUseCase);
        ReflectionTestUtils.setField(aaaxService, "timezone", "UTC");
    }

    @Test
    @DisplayName("getByUsername should delegate to authenticationService")
    void getByUsername_shouldDelegate() {
        Authentication auth = Authentication.builder().identifier("user@test.com").build();
        when(authenticationService.findValidRecordsByDynamicIdentifier("user@test.com")).thenReturn(auth);

        assertEquals(auth, aaaxService.getByUsername("user@test.com"));
    }

    @Test
    @DisplayName("get should return NA dto when user missing")
    void get_shouldReturnNaWhenMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        GetUserResponseDto dto = aaaxService.get(1L);

        assertEquals("NA", dto.getId());
        assertEquals("NA", dto.getUsername());
    }

    @Test
    @DisplayName("get and me should map existing user")
    void get_shouldMapExistingUser() {
        User user = User.builder().id(5L).username("user@test.com").status(UserStatus.ACTIVE).build();
        user.setAuthentications(List.of());
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        GetUserResponseDto dto = aaaxService.me("5");

        assertEquals("u_5", dto.getId());
        assertEquals("user@test.com", dto.getUsername());
    }

    @Test
    @DisplayName("getOne(id) should split prefixed id")
    void getOne_shouldSplitId() {
        User user = User.builder().id(9L).username("u@test.com").status(UserStatus.ACTIVE).build();
        user.setAuthentications(List.of());
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));

        GetUserResponseDto dto = aaaxService.getOne("u_9");

        assertEquals("u_9", dto.getId());
    }

    @Test
    @DisplayName("getOne with aspects should attach metrics")
    void getOne_withAspects_shouldAttachMetrics() {
        User user = User.builder().id(2L).username("u@test.com").status(UserStatus.ACTIVE).build();
        user.setAuthentications(List.of());
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        GetUserMetricsResponseDto metrics = GetUserMetricsResponseDto.builder()
                .preference(GetUserPreferenceResponseDto.builder().context(Map.of("theme", "dark")).build())
                .profile(GetUserProfileResponseDto.builder().alias("nick").build())
                .verifications(List.of(GetUserVerificationResponseDto.builder().detail(Map.of("k", "v")).build()))
                .build();
        when(userMetricsUseCase.execute("u_2", "app")).thenReturn(metrics);

        GetUserResponseDto dto = aaaxService.getOne("u_2",
                List.of(AaaxAspect.PREFERENCE, AaaxAspect.PROFILE, AaaxAspect.VERIFICATION), "app");

        assertNotNull(dto.getMetrics());
        assertTrue(dto.getMetrics().containsKey("preference"));
        assertTrue(dto.getMetrics().containsKey("profile"));
        assertTrue(dto.getMetrics().containsKey("verification"));
    }

    @Test
    @DisplayName("getOne with aspects should return NA when missing")
    void getOne_withAspects_shouldReturnNaWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        GetUserResponseDto dto = aaaxService.getOne("u_99", List.of(), null);

        assertEquals("NA", dto.getId());
    }

    @Test
    @DisplayName("getOne with invalid aspect should throw")
    void getOne_withInvalidAspect_shouldThrow() {
        User user = User.builder().id(2L).username("u@test.com").status(UserStatus.ACTIVE).build();
        user.setAuthentications(List.of());
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userMetricsUseCase.execute("u_2", null)).thenReturn(GetUserMetricsResponseDto.builder().build());

        assertThrows(BizException.class, () -> aaaxService.getOne("u_2", List.of("INVALID"), null));
    }

    @Test
    @DisplayName("getByIdentifierType should support USERNAME")
    void getByIdentifierType_shouldSupportUsername() {
        User user = User.builder().id(1L).username("alice").status(UserStatus.ACTIVE).build();
        Authentication auth = Authentication.builder().user(user).identifier("alice").loginType(LoginType.USERNAME).build();
        when(authenticationService.findValidRecordsByDynamicIdentifier("alice")).thenReturn(auth);

        GetUserResponseDto dto = aaaxService.getByIdentifierType("alice", "USERNAME");

        assertEquals("u_1", dto.getId());
    }

    @Test
    @DisplayName("getByIdentifierType should throw for unsupported type")
    void getByIdentifierType_shouldThrowForUnsupported() {
        assertThrows(BizException.class, () -> aaaxService.getByIdentifierType("x", "EMAIL"));
    }

    @Test
    @DisplayName("getUserFromIdentifier should return user")
    void getUserFromIdentifier_shouldReturnUser() {
        User user = User.builder().id(1L).username("alice").build();
        Authentication auth = Authentication.builder().user(user).build();
        when(authenticationService.findValidRecordsByDynamicIdentifier("alice")).thenReturn(auth);

        assertEquals(user, aaaxService.getUserFromIdentifier("alice"));
    }

    @Test
    @DisplayName("getById should return user or throw")
    void getById_shouldReturnOrThrow() {
        User user = User.builder().id(3L).username("u").build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        assertEquals(user, aaaxService.getById(3L));
        assertEquals(user, aaaxService.getById("u_3"));

        when(userRepository.findById(4L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> aaaxService.getById(4L));
        assertThrows(BizException.class, () -> aaaxService.getById("u_4"));
    }

    @Test
    @DisplayName("getByExtReference should return user or throw")
    void getByExtReference_shouldReturnOrThrow() {
        User user = User.builder().id(1L).build();
        when(userRepository.findByExtRef("key", "val")).thenReturn(Optional.of(user));
        assertEquals(user, aaaxService.getByExtReference("key", "val"));

        when(userRepository.findByExtRef("key", "missing")).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> aaaxService.getByExtReference("key", "missing"));
    }

    @Test
    @DisplayName("getAll should page users without filters")
    void getAll_shouldPageUsers() {
        User user = User.builder().id(1L).username("u@test.com").status(UserStatus.ACTIVE).build();
        user.setAuthentications(List.of());
        Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
        when(userRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(userProfileService.getByUserIds(List.of(1L))).thenReturn(List.of());
        when(userMetricsUseCase.execute(eq("u_1"), isNull())).thenReturn(GetUserMetricsResponseDto.builder().build());

        assertNotNull(aaaxService.getAll(
                PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createDt")),
                null, null, null, null, null, null, null, null, null));
        verify(userRepository).findAll(any(Specification.class), any(PageRequest.class));
    }
}
