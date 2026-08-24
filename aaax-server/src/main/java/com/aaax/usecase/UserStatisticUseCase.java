package com.aaax.usecase;

import com.aaax.core.response.PaginationDto;
import com.aaax.core.utils.InstantUtil;
import com.aaax.entity.enu.UaaAspect;
import com.aaax.entity.po.user.User;
import com.aaax.repository.UserRepository;
import com.aaax.repository.UserStatisticRepository;
import com.aaax.repository.projection.UserInfoProjection;
import com.aaax.service.CommonService;
import com.aaax.service.DtoWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserStatisticUseCase {

    private final UserStatisticRepository userStatisticRepository;
    private final CommonService commonService;
    private final UserRepository userRepository;
    @Value("${config.microservice.timezone:UTC}")
    private String timezone;

    public PaginationDto.PaginationDtoBuilder getUsers(Pageable pageable) {
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber() - 1, pageable.getPageSize(),
                pageable.getSort().iterator().next().getDirection(),
                pageable.getSort().iterator().next().getProperty()
        );
        Page<UserInfoProjection> userInfos = userStatisticRepository.userInfo(pageRequest);
        return DtoWrapper.getListWithPaginationResponseDto(userInfos.getContent().stream().toList(), userInfos);
    }

    public long usersCount(List<String> ss, List<String> statuses, String startDt, String endDt, String aspect) {
        Instant _startDt = StringUtils.isBlank(startDt) ? InstantUtil.parse(InstantUtil.EARLIEST_DATE) : InstantUtil.parse_tz(startDt, timezone);
        Instant _endDt = StringUtils.isBlank(endDt) ? InstantUtil.parse(InstantUtil.NEVER_EXPIRED) : InstantUtil.parse_tz(endDt, timezone);
        Specification<User> specification;
        specification = Specification.where(((root, query, builder) -> builder.between(root.get("createDt"), _startDt, _endDt)));
        List<String> _sourceSystems = Optional.ofNullable(ss).isEmpty() ? new ArrayList<>() : ss;
        if (!_sourceSystems.isEmpty()) {
            _sourceSystems.forEach(commonService::isValidSourceSystem);
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.isTrue(
                            criteriaBuilder.function("jsonb_exists_any",
                                    Boolean.class,
                                    root.get("sourceSystemTags"),
                                    criteriaBuilder.literal(ss.toArray(new String[0]))
                            )
                    )
            );
        }
        List<User> users = userRepository.findAll(specification);

        // validations
        switch (aspect) {
            case (UaaAspect.USER) -> {

            }
            case (UaaAspect.VERIFICATION) -> {

            }
        }

        return userStatisticRepository.usersCount(
                users.stream().map(User::getId).toList(),
                statuses, _startDt, _endDt, aspect);
    }
}
