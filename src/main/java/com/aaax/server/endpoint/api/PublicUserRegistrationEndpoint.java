package com.aaax.server.endpoint.api;

import com.aaax.core.entity.dto.aaax.response.GetUserResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.response.SystemResponse;
import com.aaax.server.entity.dto.request.RegisterUserRequestDto;
import com.aaax.server.entity.dto.response.PendingVerifyUserResponseDto;
import com.aaax.server.usecase.ExtraFeature;
import com.aaax.server.usecase.RegisterUserUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PublicUserRegistrationEndpoint {

    private final RegisterUserUseCase registerUserUseCase;
    @Value("${aaax.config.system-invoker}")
    protected String systemInvoker;


    // ================================ Register User Journey ===========================================
    // ================================ Step 1  ===========================================
    /**
     * Start public register (sends OTP), or availability-only when {@code check=1}.
     * <ul>
     *   <li>{@code POST /users/registrations} — occupied → 409 AAAX0409; free → 200 + OTP</li>
     *   <li>{@code POST /users/registrations?check=1} — occupied → 409 AAAX0409; free → 200,
     *       <b>no</b> OTP / no username hold (legacy FE probe)</li>
     * </ul>
     * {@code check} truthy values: {@code 1}, {@code true}, {@code yes} (case-insensitive).
     */
    @PostMapping("/users/registrations")
    public Result<PendingVerifyUserResponseDto> register(
            @Valid @RequestBody RegisterUserRequestDto requestDto,
            @RequestParam(required = false) String ss,
            @RequestParam(required = false) String check
    ) {
        requestDto.setSourceSystem(Optional.ofNullable(systemInvoker).orElse(ss));
        registerUserUseCase.registerValidations(requestDto);
        if (isRegistrationCheckOnly(check)) {
            return R.success(registerUserUseCase.register_public_checkOnly(requestDto));
        }
        return R.success(registerUserUseCase.register_public(requestDto));
    }

    private static boolean isRegistrationCheckOnly(String check) {
        if (check == null || check.isBlank()) {
            return false;
        }
        String v = check.trim();
        return "1".equals(v) || "true".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v);
    }

    // ================================ Step 2  ===========================================
    @PostMapping("/users/verifications")
    public Result<Boolean> verifyRegister(@Valid @RequestBody RegisterUserRequestDto requestDto) {
        return R.success(registerUserUseCase.verify(requestDto));
    }

    // ================================ Step 3 ===========================================
    @PutMapping("/users/registrations/one-time-passwords")
    public Result<PendingVerifyUserResponseDto> regenerateRegisterOtp(@Valid @RequestBody RegisterUserRequestDto requestDto, @RequestParam(required = false) String ss) {
        requestDto.setSourceSystem(Optional.ofNullable(systemInvoker).orElse(ss));
        registerUserUseCase.registerValidations(requestDto);
        return R.success(registerUserUseCase.regenerateRegisterOtp(requestDto));
    }

    // ================================ Step 4  ===========================================
    @PostMapping("/users")
    public Result<GetUserResponseDto> create(
            @Valid @RequestBody RegisterUserRequestDto requestDto,
            @RequestParam(required = false) List<String> f,
            @RequestParam(required = false) String ss
    ) {
        requestDto.setSourceSystem(Optional.ofNullable(systemInvoker).orElse(ss));
        this.preHandleExtraFeaturesLogic(requestDto, f);
        return R.success(registerUserUseCase.execute_external(requestDto));
    }

    // ================================ Register User Journey ===========================================


    // ================================ Register External User Journey ===========================================
    @PostMapping("/ext/users")
    public Result<GetUserResponseDto> register_external_noOtp(
            @Valid @RequestBody RegisterUserRequestDto requestDto,
            @RequestParam(required = false) List<String> f
    ) {
        this.preHandleExtraFeaturesLogic(requestDto, f);
        return R.success(registerUserUseCase.execute(requestDto));
    }
    // ================================ Register External User Journey ===========================================


    // ================================ COMMON ===========================================
    private void preHandleExtraFeaturesLogic(RegisterUserRequestDto requestDto, List<String> f) {
        List<String> features = Optional.ofNullable(f).orElse(List.of());
        boolean isContained = features.stream().anyMatch(ExtraFeature.ALL::contains);
        if (isContained) {
            requestDto.setExtraFeatures(features);
        }

        // ==== VALIDATIONS
        if (Optional.ofNullable(requestDto.getMetadata()).isPresent()) {
            if (Optional.ofNullable(requestDto.getMetadata().get("phone")).isPresent()) {
                if (Optional.ofNullable(requestDto.getMetadata().get("areaCode")).isEmpty()) {
                    throw new BizException(SystemResponse.PAM0400, "Plz provide [areaCode]");
                }
            }
        }
    }
    // ================================ COMMON ===========================================
}
