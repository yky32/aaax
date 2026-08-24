package com.aaax.endpoint.api.authentication;

import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.utils.ValidationUtil;
import com.aaax.entity.dto.json_context.OtpMetadata;
import com.aaax.entity.dto.request.CreateOtpRequestDto;
import com.aaax.entity.dto.request.VerifyOtpRequestDto;
import com.aaax.usecase.otp.OtpUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/authentications")
public class OtpEndpoint {

    private final OtpUseCase otpUseCase;

    // ===== OTP_GENERAL ====
    @PostMapping("/one-time-passwords/general")
    public Result<OtpMetadata> generate(@RequestBody CreateOtpRequestDto requestDto) {
        return R.success(otpUseCase.generate(requestDto, "general"));
    }
    @PutMapping("/one-time-passwords/general")
    public Result<OtpMetadata> re_generate(@RequestBody CreateOtpRequestDto requestDto) {
        return R.success(otpUseCase.re_generate(requestDto));
    }
    @PostMapping("/one-time-passwords/general/verifications")
    public Result<Boolean> verify(@RequestBody VerifyOtpRequestDto requestDto) {
        ValidationUtil.nonEmptyNonNull(requestDto.getCode(), "code");
        return R.success(otpUseCase.verify(requestDto));
    }
    // ===== OTP_RESET_PASSWORD ====
}
