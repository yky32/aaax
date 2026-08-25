package com.aaax.core.aop.authorization;


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
public @interface RequiredPermission {
    String[] authorities();
}
