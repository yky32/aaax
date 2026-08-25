package com.aaax.endpoint.api.cache;

import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CacheEndpoint {

    private final RedisUtil redisUtil;

    @DeleteMapping("/caches")
    public Result delete(
            @RequestParam String prefix,
            @RequestParam(required = false) String query
    ) {
        // FIXME: later need to fix for api permission
        Set<String> keys = redisUtil.getListByWildCard(prefix);
        for (String key : keys) {
            log.info("--- key => {}", key);
            redisUtil.delete(key);
        }
        return R.success();
    }

    @GetMapping("/caches")
    public Result get(
            @RequestParam String prefix,
            @RequestParam(required = false) String query
    ) {
        // FIXME: later need to fix for api permission
        if (StringUtils.isNotBlank(query)) {
            prefix.concat(":").concat(query);
        }
        Set<String> keys = redisUtil.getListByWildCard(prefix);
        Map<String, Object> result = new HashMap<>();
        for (String key : keys) {
            result.put(key, redisUtil.get(key));
        }
        return R.success(result);
    }
}
