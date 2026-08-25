package com.aaax.server.endpoint.api.forgot_password;

import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.server.entity.dto.request.ForgotPasswordRequestDto;
import com.aaax.server.entity.dto.response.PendingVerifyUserResponseDto;
import com.aaax.server.usecase.ResetPasswordUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordEndpoint {

    private final ResetPasswordUseCase resetPasswordUseCase;
    @Value("${config.system-invoker}")
    protected String systemInvoker;


    // ================================ Forgot Password Journey ===========================================
    // ================================ Step 1  ===========================================
    @PostMapping("/users/credentials/reset")
    public Result<PendingVerifyUserResponseDto> initiate(@Valid @RequestBody ForgotPasswordRequestDto dto, @RequestParam(required = false) String ss) {
        dto.setSourceSystem(Optional.ofNullable(ss).isEmpty() ? systemInvoker : ss);
        resetPasswordUseCase.forgotPasswordValidation(dto);
        log.info("========= resetPasswordUseCase.initiate request with @@@ \n {}", dto);
        PendingVerifyUserResponseDto responseDto = resetPasswordUseCase.initiate(dto);
        log.info("========= resetPasswordUseCase.initiated OTP with @@@ \n {}", responseDto);
        return R.success(responseDto);
    }

    // ================================ Step 2  ===========================================
    @PostMapping("/users/credentials/reset/validations")
    public Result<Boolean> validate(@Valid @RequestBody ForgotPasswordRequestDto dto) {
        return R.success(resetPasswordUseCase.validate(dto));
    }

    // ================================ Step 3 ===========================================
    @PutMapping("/users/credentials/reset/one-time-passwords")
    public Result<PendingVerifyUserResponseDto> regenerateRegisterOtp(@Valid @RequestBody ForgotPasswordRequestDto dto, @RequestParam(required = false) String ss) {
        dto.setSourceSystem(Optional.ofNullable(ss).isEmpty() ? systemInvoker : ss);
        resetPasswordUseCase.forgotPasswordValidation(dto);
        return R.success(resetPasswordUseCase.regenerateOtp(dto));
    }

    // ================================ Step 4  ===========================================
    @PatchMapping("/users/credentials")
    public Result<GetUserResponseDto> create(@Valid @RequestBody ForgotPasswordRequestDto requestDto) {
        return R.success(resetPasswordUseCase.updateNewPassword(requestDto));
    }
    // ================================ Forgot Password Journey ===========================================
}
