package com.aaax.core.aop.log;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * topic = ACTIVITY_LOG_TRANSACTION_STARTED,
 * logScope = ENDPOINT, SERVICE, REPOSITORY etc.
 * domain = "payments",
 * event = "refund",
 * scope = "INT", "EXT
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ActivityLog {
    String topic();

    LogScope logScope();

    String domain();

    String event();

    String scope();
}
