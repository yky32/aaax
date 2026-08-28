package com.aaax.server.usecase;

import com.aaax.core.entity.dto.aaax.response.GetUserResponseDto;
import com.aaax.core.utils.IdSplitter;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.entity.po.UserRoute;
import com.aaax.server.repository.AuthenticationRepository;
import com.aaax.server.repository.UserRepository;
import com.aaax.server.repository.UserRouteRepository;
import com.aaax.server.service.DtoWrapper;
import com.aaax.server.service.AaaxService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class GetUserUseCase {

    private final PasswordEncoder passwordEncoder;
    private final AaaxService aaaxService;
    private final UserRepository userRepository;
    private final AuthenticationRepository authenticationRepository;
    private final UserRouteRepository userRouteRepository;

    public GetUserResponseDto execute(Long userId) {
        return aaaxService.get(userId);
    }

    /**
     * @param identifier - username, to
     * @return Authentication.class
     */
    public Authentication execute(String identifier) {
        Authentication authentication = aaaxService.getByUsername(identifier);
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
