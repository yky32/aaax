package com.aaax.usecase;

import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.config.redis.RedisKey;
import com.aaax.config.security.jwt.Jwt;
import com.aaax.entity.dto.request.QrCodeLoginRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QrCodeLoginUseCase {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisUtil redisUtil;

    public void qrCodeLogin(QrCodeLoginRequestDto requestDto) {
        JwtAuthenticationToken authentication = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("-- 1. get userId from auth userId: {}", userId);
        Jwt jwt = JSONUtil.convertFromObject(redisUtil.getOrElseThrow(RedisKey.USER_OAUTH_TOKENS.getKey().concat(userId)), Jwt.class);
        String sessionId = (String) redisUtil.getOrElseThrow(RedisKey.DEVICE_SESSIONS.getKey().concat(requestDto.getDeviceId()));
        log.info("-- 2. get sessionId: {}", sessionId);
        stringRedisTemplate.convertAndSend(sessionId, jwt.getRefreshToken());
    }
}
