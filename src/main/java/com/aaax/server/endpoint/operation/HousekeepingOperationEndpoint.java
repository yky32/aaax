package com.aaax.server.endpoint.operation;

import com.aaax.core.kafka.enu.KafkaTopic;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.server.entity.dto.response.ClientResponseDto;
import com.aaax.server.entity.po.user_token.UserToken;
import com.aaax.server.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Api Key Management
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/operations")
@Slf4j
public class HousekeepingOperationEndpoint {

    private final UserTokenRepository userTokenRepository;
    private final KafkaUtil kafkaUtil;

    @DeleteMapping("/housekeeping/user-tokens")
    public Result<Map> housekeepingUserTokens() {
        log.info("-- Housekeeping user tokens");
        // find expireAt is expired
        Pageable pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "createDt") );
        List<UserToken> expiredUserTokens = userTokenRepository.findByExpireAtBefore(Instant.now(), pageable);
        for (UserToken expiredUserToken : expiredUserTokens) {
            log.info("-- Housekeeping user tokens - in progress");
            kafkaUtil.send(KafkaTopic.USER_HOUSEKEEPING_EXPIRED_USER_TOKENS,
                    Map.of("id",expiredUserToken.getId())
            );
        }
        log.info("-- Housekeeping user tokens - completed");
        return R.success(Map.of("message", "User tokens housekeeping completed", "count", expiredUserTokens.size()));
    }

}
