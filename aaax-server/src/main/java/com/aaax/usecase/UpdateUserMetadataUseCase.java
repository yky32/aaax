package com.aaax.usecase;

import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.utils.RedisUtil;
import com.aaax.config.redis.RedisKey;
import com.aaax.entity.dto.request.UpdateAccessMetadataRequestDto;
import com.aaax.entity.dto.request.UpdateExtReferenceRequestDto;
import com.aaax.entity.po.user.User;
import com.aaax.repository.UserRepository;
import com.aaax.service.DtoWrapper;
import com.aaax.service.UaaService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@AllArgsConstructor
@Slf4j
public class UpdateUserMetadataUseCase {

    private final UserRepository userRepository;
    private final UaaService uaaService;
    private final RedisUtil redisUtil;

    public GetUserResponseDto updateExtReference(UpdateExtReferenceRequestDto requestDto, Long userId) {
        User user = uaaService.getById(userId);
        user.getMetadata().setExtReferenceMap(requestDto.getExtReferenceMap());
        user = userRepository.save(user);
        return DtoWrapper.getUserResponseDto(user, user.getAuthentications());
    }

    public GetUserResponseDto updateAccess(UpdateAccessMetadataRequestDto requestDto, String userId) {
        Objects.requireNonNull(requestDto.getAccess(), "[metadata.access] cannot be null.");
        User user = uaaService.getById(userId);
        user = userRepository.saveAndFlush(user);

        // refresh cache in redis ==> to be safe ==> all clear.
        Set<String> keys = redisUtil.getListByWildCard(RedisKey.LOGIN_MY_TENANTS.getKey());
        for (String key : keys) {
            log.info("--- cleanup redis already => [{}] @ {}", key, user.getUsername());
            redisUtil.delete(key);
        }
        return DtoWrapper.getUserResponseDto(user, user.getAuthentications());
    }
}
