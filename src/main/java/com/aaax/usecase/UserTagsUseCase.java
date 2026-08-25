package com.aaax.usecase;

import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.utils.IdSplitter;
import com.aaax.entity.po.user.User;
import com.aaax.exception.response.UaaErrorResponse;
import com.aaax.repository.UserRepository;
import com.aaax.service.DtoWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserTagsUseCase {

    private final UserRepository userRepository;

    public GetUserResponseDto assign(String userId, String ss) {
        User user = userRepository.findById(Long.valueOf(IdSplitter.split(userId))).orElseThrow(() -> new BizException(UaaErrorResponse.UAA0001, Map.of("id", userId)));
        Set<String> tags = new HashSet<>(Optional.ofNullable(user.getSourceSystemTags()).orElse(new ArrayList<>()));
        tags.add(ss);
        user.setSourceSystemTags(new ArrayList<>(tags));
        user = userRepository.save(user);
        return DtoWrapper.getUserResponseDto(user, user.getAuthentications());
    }
}
