package com.aaax.config.aop.log;

import com.aaax.core.common.AppContext;
import com.aaax.core.common.AppContextHolder;
import com.aaax.core.kafka.event.LogCreatedEvent;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.config.aop.BaseAspect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import java.util.UUID;


/**
 * Core-logic for produce the log.
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class ActivityLogAspect extends BaseAspect {

    private final KafkaUtil kafkaUtil;
    @Before("@annotation(ActivityLog)")
    public void activityLog_actionForBefore(JoinPoint joinPoint) {
        this.actionForBefore("ActivityLog", joinPoint);
    }


    @Override
    public void executeForBefore(JoinPoint joinPoint) {
        AppContext appContext = AppContextHolder.CONTEXT.get();
        kafkaUtil.send(
                appContext.getLogContext().getTopic(),
                LogCreatedEvent.builder()
                        .requestContext(appContext.getRequestContext())
                        .logContext(appContext.getLogContext())
                        .requestId(UUID.randomUUID().toString())
                        .eventName(appContext.getLogContext().getTopic())
                        .build()
        );
    }

    @After("@annotation(ActivityLog)")
    public void activityLog_actionForAfter(JoinPoint joinPoint) {
        super.actionForAfter("ActivityLog", joinPoint);
    }

    @Override
    public void executeForAfter(JoinPoint joinPoint) {
        super.executeForAfter(joinPoint);
    }
}
