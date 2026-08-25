package com.aaax.server.usecase;

import com.aaax.core.response.PaginationDto;
import com.aaax.core.utils.IdSplitter;
import com.aaax.core.utils.InstantUtil;
import com.aaax.server.entity.dto.response.GetAuthenticationLogResponseDto;
import com.aaax.server.entity.po.log.AuthenticationLog;
import com.aaax.server.repository.AuthenticationLogRepository;
import com.aaax.server.service.DtoWrapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists {@link AuthenticationLog} rows for a single user (login success / fail trail).
 */
@Component
@RequiredArgsConstructor
public class QueryUserAuthenticationLogsUseCase {

    private final AuthenticationLogRepository authenticationLogRepository;

    public PaginationDto.PaginationDtoBuilder execute(
            String userId,
            Pageable pageable,
            String startDt,
            String endDt,
            String event
    ) {
        Instant start = StringUtils.isBlank(startDt)
                ? InstantUtil.parse(InstantUtil.EARLIEST_DATE)
                : InstantUtil.parse_tz(startDt, "UTC");
        Instant end = StringUtils.isBlank(endDt)
                ? InstantUtil.parse(InstantUtil.NEVER_EXPIRED)
                : InstantUtil.parse_tz(endDt, "UTC");

        List<String> userKeys = resolveUserKeys(userId);

        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "createDt");
        PageRequest pageRequest = PageRequest.of(
                Math.max(0, pageable.getPageNumber() - 1),
                pageable.getPageSize(),
                sort
        );

        Specification<AuthenticationLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(root.get("createDt"), start, end));
            predicates.add(cb.or(
                    root.get("actionBy").in(userKeys),
                    root.get("correlationId").in(userKeys)
            ));
            if (StringUtils.isNotBlank(event)) {
                predicates.add(cb.equal(root.get("event"), event.trim()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<AuthenticationLog> page = authenticationLogRepository.findAll(spec, pageRequest);
        List<GetAuthenticationLogResponseDto> data = page.getContent().stream()
                .map(DtoWrapper::getAuthenticationLogResponseDto)
                .toList();
        return DtoWrapper.getListWithPaginationResponseDto(data, page);
    }

    /**
     * Match both {@code u_123} and bare {@code 123} forms used across login workers.
     */
    static List<String> resolveUserKeys(String userId) {
        if (StringUtils.isBlank(userId)) {
            return List.of();
        }
        String raw = userId.trim();
        String bare = IdSplitter.split(raw);
        if (raw.equals(bare)) {
            return List.of(raw, "u_" + bare);
        }
        return List.of(raw, bare);
    }
}
