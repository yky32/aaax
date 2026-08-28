package com.aaax.core.filter;

import com.aaax.core.common.AppContext;
import com.aaax.core.common.AppContextHolder;
import com.aaax.core.common.jsonfield.LogContextMetadata;
import com.aaax.core.common.jsonfield.RequestContextMetadata;
import com.aaax.core.constant.HttpHeader;
import com.aaax.core.exception.BizException;
import com.aaax.core.redis.CoreRedisKey;
import com.aaax.core.response.R;
import com.aaax.core.response.SystemResponse;
import com.aaax.core.utils.InstantUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.core.utils.handler.EndpointHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

/**
 * this is the filter to manage [microservice] related issue. example,
 * 1. x-request-id - for global traceid
 * 2. x-app-id ??
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Builder
@Data
public class AppFilter extends BaseFilter {

    private String serviceName;
    private RedisUtil redisUtil;
    @Value("${aaax.config.server.rate-limit-counter:30}")
    private Integer rateLimitCounter;
    @Value("${aaax.config.server.rate-limit-interval:60}")
    private Integer rateLimitInterval; // # seconds
    private Map<String, Integer> rateLimitCounterConfig;
    private List<String> byPassPaths;

    public AppFilter(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("Please Specify the [serviceName] in [AppFilter].");
        }
        this.serviceName = serviceName;
    }

    public AppFilter(
            String serviceName, RedisUtil redisUtil, Integer rateLimitCounter, Integer rateLimitInterval,
            Map<String, Integer> rateLimitCounterConfig,
            List<String> byPassPaths
    ) {
        if (serviceName == null) {
            throw new IllegalArgumentException("Please Specify the [serviceName] in [AppFilter].");
        }
        this.serviceName = serviceName;
        this.redisUtil = redisUtil;
        this.rateLimitCounter = rateLimitCounter;
        this.rateLimitInterval = rateLimitInterval;
        this.rateLimitCounterConfig = rateLimitCounterConfig;
        this.byPassPaths = byPassPaths;
    }

    public AppFilter(String serviceName, RedisUtil redisUtil) {
        if (serviceName == null) {
            throw new IllegalArgumentException("Please Specify the [serviceName] in [AppFilter].");
        }
        this.serviceName = serviceName;
        this.redisUtil = redisUtil;
    }

    @Override
    @SneakyThrows
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        String traceId;
        if (!StringUtils.isEmpty(HttpHeader.X_REQUEST_ID) && !StringUtils.isEmpty(request.getHeader(HttpHeader.X_REQUEST_ID))) {
            traceId = request.getHeader(HttpHeader.X_REQUEST_ID);
        } else {
            traceId = serviceName.concat("-").concat(UUID.randomUUID().toString().replace("-", ""));
        }
        try {
            // adding [x-request-id] to the response headers
            response.addHeader(HttpHeader.X_REQUEST_ID, traceId);
            // replacing [traceKey] in MDC with the trace-id of each request
            MDC.put(HttpHeader.X_REQUEST_ID, response.getHeader(HttpHeader.X_REQUEST_ID));
            log.info("-- API call >>>>>>>> : {} : {} by {}", request.getMethod(), request.getRequestURI(), traceId);
            // executing the request once the headers and trace-id modifications are done
            // ====== Setup [AppContext]
            this.setupAppContext(request, traceId);
            boolean isTurnOnRateLimit = this.rateLimitTurnOn(traceId);
            if (isTurnOnRateLimit) {
                throw new BizException(SystemResponse.SYS9429);
            }
            chain.doFilter(request, response);
        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof BizException) {
                EndpointHandler.out(response, ((BizException) ex).getResponse().getHttpStatus().value(), R.fail(((BizException) ex).getResponse(), ((BizException) ex).getData()));
            } else {
                Map<String, Object> detail = Map.of("path", request.getRequestURI(), "error", ex.getMessage());
                String message = String.format("Plz contact system admin, %s", ex.getMessage());
                detail.put("message", message);
                EndpointHandler.out(response, HttpStatus.INTERNAL_SERVER_ERROR.value(), R.fail(detail));
            }
        } finally {
            // clearing the MDC once the request is complete
            MDC.clear();
            log.info("-- API call end<<<<<<: {} : {} returning [{}] by {}", request.getMethod(), request.getRequestURI(), response.getStatus(), traceId);
            AppContextHolder.clearContext();
            log.info("-- AppFilter, [TenantContextHolder, AppContextHolder]->clearContext()");
        }
    }

    private boolean rateLimitTurnOn(String traceId) {
        if (redisUtil == null) return false; // by-pass, 不啟動
        boolean decision;
        try {
            // ==== START Checking ====
            AppContext appContext = AppContextHolder.CONTEXT.get();
            // special handling for some ip...
            String ipKey = CoreRedisKey.SYSTEM_IP.getKey()
                    .concat(appContext.getRequestContext().getIp())
                    .concat(":")
                    .concat(appContext.getRequestContext().getApiUrl())
                    ;
            log.info("-- API call rateLimitTurnOn: {}-{}", ipKey, traceId);
            int counter = 1;
            if (!redisUtil.hasKey(ipKey)) {
                redisUtil.set(ipKey, counter, rateLimitInterval);
            } else{
                counter = (int) redisUtil.increment(ipKey, 1);
            }
            log.info("-- API call rateLimitTurnOn counter : {} @@@ {} by {}-{}", counter, ipKey, Instant.now(), traceId);
            decision = (counter >= rateLimitCounterConfig(appContext.getRequestContext().getApiUrl()));
        } catch (Exception exception) {
            log.info("-- API call rateLimitTurnOn in try-catch block: {} - {} ", exception.getMessage(), traceId);
            decision = false; // in-case redis-dead. still can be resumed to normal, 包底
        }
        return decision;
    }

    private int rateLimitCounterConfig(String apiUrl) {
        log.info("-- API call normal counter : {}", rateLimitCounter);
        if (rateLimitCounterConfig == null) {
            return rateLimitCounter;
        }
        // Iterate over the entries in the map
        for (Map.Entry<String, Integer> entry : rateLimitCounterConfig.entrySet()) {
            if (apiUrl.contains(entry.getKey())) {
                log.info("-- API call special counter : {} by {}", entry.getKey(), entry.getValue());
                return entry.getValue();
            }
        }
        return rateLimitCounter;
    }

    private void setupAppContext(HttpServletRequest request, String traceId) {
        this.doRequestAudit(request);
        String userAgentString = request.getHeader("User-Agent");
        String clientIp = getClientIp(request);
        AppContext appContext = AppContext.builder()
                .requestContext(
                        RequestContextMetadata.builder()
                                .userAgent(userAgentString)
                                .ip(clientIp)
                                .startDt(InstantUtil.parse_tz(Instant.now(), ZoneOffset.UTC))
                                .apiUrl(("@".concat(request.getMethod())).concat(request.getRequestURI()))
                                .requestId(traceId)
                                .requestHeaders(collectRequestHeaders(request))
                                .build()
                )
                .logContext(
                        LogContextMetadata.builder()
                                .build()
                ).build();
        AppContextHolder.CONTEXT.set(appContext);
        log.info("-- AppFilter, setupAppContext => {}", appContext);
    }

    private void doRequestAudit(HttpServletRequest request) {
        String userAgentString = request.getHeader("User-Agent");
        log.info("-- doRequestAudit userAgent : {}", userAgentString);
        log.info("-- doRequestAudit getServerName: {}", request.getServerName());
        log.info("-- doRequestAudit getRemoteHost: {}", request.getRemoteHost());
        log.info("-- doRequestAudit getRequestURI: {}", request.getRequestURI());

        // by-pass checking
        List<String> urls = Optional.ofNullable(byPassPaths).orElse(new ArrayList<>());
        if (!urls.isEmpty()) {
            boolean contains = urls.stream().anyMatch(url -> {
                log.info("-- doRequestAudit check [pass-in uri]: {} vs [trusted uri]: {} ", url, request.getRequestURI());
                log.info("-- doRequestAudit check [pass-in sn]: {} vs [trusted sn]: {} ", url, request.getServerName());
                return request.getRequestURI().contains(url) || request.getServerName().contains(url);
            });
            if (contains) {
                log.info("-- doRequestAudit byPass-ed : {} @ {}", request.getServerName(), request.getRequestURI());
                return;
            }
            log.info("-- doRequestAudit byPass Mis-match : {} @ {}", request.getServerName(), request.getRequestURI());
        }

        if (userAgentString == null || userAgentString.isEmpty()) {
            throw new BizException(SystemResponse.SYS9405, "[UA] cannot be null.");
        }

        log.info("-- doRequestAudit back to normal : {} @ {}", request.getServerName(), request.getRequestURI());
    }

    private String getClientIp(HttpServletRequest request) {
        String x_forwarded_for = request.getHeader("X-Forwarded-For");
        if (x_forwarded_for != null) {
            log.info("-- getClientIp x_forwarded_for , {}", x_forwarded_for);
            return x_forwarded_for;
        }

        String x_real_ip = request.getHeader("X-Real-IP");
        if (x_real_ip != null) {
            log.info("-- getClientIp x_real_ip , {}", x_real_ip);
            return x_real_ip;
        }
        String remoteAddr = request.getRemoteAddr();
        log.info("-- getClientIp remoteAddr , {}", remoteAddr);
        return remoteAddr;
    }

    private Map<String, String> collectRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.put(headerName, headerValue);
        }
        return headers;
    }
}