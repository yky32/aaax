package com.aaax.core.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the caller to hold at least one of the given live AAAX roles
 * ({@code GET /users/my-roles}), checked before the endpoint runs.
 * <p>
 * When {@link #value()} is empty, falls back to {@code app.tgt.operator.role}
 * (default {@link OperatorRoles#ADMIN}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiredRoles {
    String[] value() default {};
}
