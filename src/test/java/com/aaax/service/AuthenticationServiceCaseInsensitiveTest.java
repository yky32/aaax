package com.aaax.service;

import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.exception.BizException;
import com.aaax.entity.po.user.Authentication;
import com.aaax.entity.po.user.User;
import com.aaax.exception.response.UaaErrorResponse;
import com.aaax.repository.AuthenticationRepository;
import com.aaax.repository.UserRepository;
import com.aaax.usecase.otp.ForgotPasswordOtpUseCase;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RedisUtil;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceCaseInsensitiveTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthenticationRepository authenticationRepository;
    @Mock private PasswordEncoder encoder;
    @Mock private ForgotPasswordOtpUseCase forgotPasswordOtpUseCase;
    @Mock private KafkaUtil kafkaUtil;
    @Mock private RedisUtil redisUtil;
    @Mock private RSAKey rsaKey;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("public register: mixed-case email matches existing → UAA0409")
    void existed_mixedCase_throws409() {
        User user = User.builder().id(1L).username("acekaiyin@gmail.com").status(UserStatus.ACTIVE).build();
        Authentication auth = Authentication.builder()
                .id(10L)
                .identifier("acekaiyin@gmail.com")
                .loginType(LoginType.EMAIL)
                .user(user)
                .build();
        auth.setIsActive(true);
        when(authenticationRepository.findAllByIdentifierIgnoreCase("acekaiyin@gmail.com"))
                .thenReturn(List.of(auth));

        BizException ex = assertThrows(BizException.class,
                () -> authenticationService.isThisUsernameExistedForPublicRegister("Acekaiyin@gmail.com"));
        assertEquals(UaaErrorResponse.UAA0409.getCode(), ex.getResponse().getCode());
    }

    @Test
    @DisplayName("lookup by mixed-case email finds row")
    void findOptional_ignoreCase() {
        Authentication auth = Authentication.builder()
                .identifier("admin@tgt.gg")
                .loginType(LoginType.EMAIL)
                .build();
        auth.setIsActive(true);
        when(authenticationRepository.findByIdentifierIgnoreCaseAndLoginType(eq("admin@tgt.gg"), eq(LoginType.EMAIL)))
                .thenReturn(Optional.of(auth));

        Optional<Authentication> found = authenticationService.findOptionalByDynamicIdentifier("ADmin@tgt.gg");
        assertTrue(found.isPresent());
        assertEquals("admin@tgt.gg", found.get().getIdentifier());
    }
}
