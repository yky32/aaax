package com.aaax.server.entity.dto.request;

import com.aaax.server.config.redis.RedisKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VerifyOtpRequestDto {
    private String to;
    private String code;
    private RedisKey usecase;
}
