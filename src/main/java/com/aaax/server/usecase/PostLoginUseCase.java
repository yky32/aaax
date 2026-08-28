package com.aaax.server.usecase;

import com.aaax.core.entity.dto.aaax.response.GetUserResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.utils.IdSplitter;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.exception.response.AaaxErrorResponse;
import com.aaax.server.repository.UserRepository;
import com.aaax.server.service.DtoWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostLoginUseCase {

    private final UserRepository userRepository;

    public GetUserResponseDto assign(String userId, String ss) {
        User user = userRepository.findById(Long.valueOf(IdSplitter.split(userId))).orElseThrow(() -> new BizException(AaaxErrorResponse.AAAX0001, Map.of("id", userId)));
        Set<String> tags = new HashSet<>(Optional.ofNullable(user.getSourceSystemTags()).orElse(new ArrayList<>()));
        tags.add(ss);
        user.setSourceSystemTags(new ArrayList<>(tags));
        user = userRepository.save(user);
        return DtoWrapper.getUserResponseDto(user, user.getAuthentications());
    }
}
