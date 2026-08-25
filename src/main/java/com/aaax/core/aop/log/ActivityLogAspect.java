package com.aaax.core.aop.log;

import com.aaax.core.aop.BaseAspect;
import com.aaax.core.common.AppContext;
import com.aaax.core.common.AppContextHolder;
import com.aaax.core.common.jsonfield.LogContextMetadata;
import com.aaax.core.common.jsonfield.RequestContextMetadata;
import com.aaax.core.constant.enu.LogType;
import com.aaax.core.kafka.event.LogCreatedEvent;
import com.aaax.core.response.Result;
import com.aaax.core.utils.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotationUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;


/**
 * Core-logic for produce the log.
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class ActivityLogAspect extends BaseAspect {

    @Value("${spring.application.name}")
    private String serviceName;
    private final HttpServletRequest request;

    private final KafkaUtil kafkaUtil;

    @Before("@annotation(ActivityLog)")
    public void activityLog_actionForBefore(JoinPoint joinPoint) {
        this.actionForBefore("ActivityLog", joinPoint);
    }

    @Override
    public void executeForBefore(JoinPoint joinPoint) {
    }

    @AfterThrowing(value = "@annotation(ActivityLog)", throwing = "ex")
    public void activityLog_actionForAfter(JoinPoint joinPoint, Exception ex) {
        super.actionForAfter("ActivityLog", joinPoint, ex);
    }

    @AfterReturning(value = "@annotation(ActivityLog)", returning = "result")
    public void activityLog_actionForAfter(JoinPoint joinPoint, Object result) {
        super.actionForAfter("ActivityLog", joinPoint, result);
    }

    @Override
    public void executeForAfter(JoinPoint joinPoint, Object result) {
        ActivityLog activityLog = AnnotationUtils.getAnnotation(ReflectionUtil.getMethod(joinPoint), ActivityLog.class);
        AppContext appContext = AppContextHolder.CONTEXT.get();
        if (appContext == null) {
            appContext = AppContext.builder()
                    .requestContext(
                            RequestContextMetadata.builder()
                                    .startDt(InstantUtil.parse_tz(Instant.now(), ZoneOffset.UTC))
                                    .apiUrl(("@".concat("request.getMethod()")).concat("request.getRequestURI()"))
                                    .requestId(UUID.randomUUID().toString())
                                    .requestHeaders("requestHeaders")
                                    .build()
                    )
                    .logContext(
                            LogContextMetadata.builder()
                                    .build()
                    ).build();
            AppContextHolder.CONTEXT.set(appContext);
        }
        setUp(appContext, joinPoint);

        // == process the return [value].
        // == construct the [LogContextMetadata]
        LogContextMetadata logContext = LogContextMetadata.builder().build();
        logContext.setActionBy(Optional.of(JwtUtil.userId()).filter(s -> !"0".equals(s)).orElse("==Error"));
        logContext.setTopic(Objects.requireNonNull(activityLog).topic());
        logContext.setRequestContext(appContext.getRequestContext());
        logContext.setType(LogType.ACTIVITY);
        logContext.setLogScope(activityLog.logScope());
        logContext.setScope(activityLog.scope());
        logContext.setSystem(serviceName);
        logContext.setDomain(activityLog.domain());
        logContext.setEvent(activityLog.domain().concat(".").concat(activityLog.event()));
        logContext.setTrafficTimeInMilliseconds(Duration.between(InstantUtil.parse_tz(appContext.getRequestContext().getStartDt(), ZoneOffset.UTC), InstantUtil.parse_tz(appContext.getRequestContext().getEndDt(), ZoneOffset.UTC)).toMillis());

        // This ID need to be set by different implementation, by [invoker] ==============
        logContext.setRequestBody(appContext.getLogContext().getRequestBody());
        logContext.setResponseBody(appContext.getLogContext().getResponseBody());
        logContext.setCorrelationId(appContext.getLogContext().getCorrelationId());
        // This ID need to be set by different implementation, by [invoker] ==============

        if (result instanceof Exception) {
            Map error = JSONUtil.convertFromObject(result, Map.class);
            List<ArrayList> stackTrace = (List<ArrayList>) error.getOrDefault("stackTrace", new ArrayList<>());
            if (!stackTrace.isEmpty()) {
                error.put("trace", stackTrace.get(0));
            }
            error.remove("stackTrace");
            error.remove("suppressed");
            logContext.setContent(result);
        } else {
            log.info("-- ActivityLogAspect.logContext entering the switch case => \n" +
                    "[logScope] => {} \n" +
                    "[domain] => {} \n" +
                    "[event] => {} \n" +
                    "[result] => {} " ,
                    activityLog.logScope(),
                    activityLog.domain(),
                    activityLog.event(),
                    result
            );
            switch (Objects.requireNonNull(activityLog).logScope()) {
                case ENDPOINT -> {
                    Result<Map> response = (Result<Map>) JSONUtil.convertFromObject(result, Result.class);
                    logContext.setContent(response.getData());
                    // take out the id.
                    String id = (String) response.getData().get("id");
                    logContext.setTraceId(IdSplitter.split(id));
                }
                case SERVICE, API, REPOSITORY -> {
                    Map response = JSONUtil.convertFromObject(result, Map.class);
                    logContext.setContent(result);
                    String id = (String) response.get("id");
                    logContext.setTraceId(IdSplitter.split(id));
                }
            }
        }
        // ==== final set
        appContext.setLogContext(logContext);

        // == take action ==
        LogCreatedEvent event = LogCreatedEvent.builder()
                .requestContext(appContext.getRequestContext())
                .logContext(appContext.getLogContext())
                .requestId(Optional.ofNullable(request.getHeader("x-request-id")).orElse(UUID.randomUUID().toString()))
                .eventName(appContext.getLogContext().getTopic())
                .build();

        // == take action ==
        log.info("-- ActivityLogAspect.traceId => {} - {}", logContext.getTraceId(), logContext.getCorrelationId());
        kafkaUtil.send(AppContextHolder.CONTEXT.get().getLogContext().getTopic(), event);
        log.info("-- ActivityLogAspect.logContext end => {} event => {}", logContext, event);
        // == take action ==
    }


    // ========= util method

    private void setUp(AppContext appContext, JoinPoint joinPoint) {
        Object[] methodArgs = joinPoint.getArgs();
        if (methodArgs.length > 0) {
            appContext.getRequestContext().setMethodArguments(joinPoint.getArgs());
        }
        ActivityLog activityLog = AnnotationUtils.getAnnotation(ReflectionUtil.getMethod(joinPoint), ActivityLog.class);
        appContext.getLogContext().setTopic(Objects.requireNonNull(activityLog).topic());
        appContext.getRequestContext().setEndDt(InstantUtil.parse_tz(Instant.now(), ZoneOffset.UTC));
    }
}
