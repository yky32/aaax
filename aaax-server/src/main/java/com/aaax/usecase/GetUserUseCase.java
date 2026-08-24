package com.aaax.usecase;

import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.utils.IdSplitter;
import com.aaax.entity.po.user.Authentication;
import com.aaax.entity.po.user.User;
import com.aaax.entity.po.UserRoute;
import com.aaax.repository.AuthenticationRepository;
import com.aaax.repository.UserRepository;
import com.aaax.repository.UserRouteRepository;
import com.aaax.service.DtoWrapper;
import com.aaax.service.UaaService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class GetUserUseCase {

    private final PasswordEncoder passwordEncoder;
    private final UaaService uaaService;
    private final UserRepository userRepository;
    private final AuthenticationRepository authenticationRepository;
    private final UserRouteRepository userRouteRepository;

    public GetUserResponseDto execute(Long userId) {
        return uaaService.get(userId);
    }

    /**
     * @param identifier - username, to
     * @return Authentication.class
     */
    public Authentication execute(String identifier) {
        Authentication authentication = uaaService.getByUsername(identifier);
        return authentication;
    }

    public List<GetUserResponseDto> searchByTrrIds(List<String> trrIds) {
        List<UserRoute> userRoutes = userRouteRepository.findAllByTenantRoleRouteIdIn(trrIds.stream().map(Long::valueOf).toList());
        List<Long> userIds = userRoutes.stream().map(UserRoute::getUserId).toList();
        List<User> users = userRepository.findAllById(userIds);
        return users.stream().map(user -> DtoWrapper.getUserResponseDto(user, user.getAuthentications())).toList();
    }

    public List<Long> getTrrIds(String userId) {
        List<UserRoute> userRoutes = userRouteRepository.findAllByUserId(Long.valueOf(IdSplitter.split(userId)));
        return userRoutes.stream().map(UserRoute::getTenantRoleRouteId).toList();
    }
}
