package com.aaax.core.api;

/**
 * Dynamically for embedded different handlers implementation
 *
 * @param <T> - TenantContext Stored in ThreadLocal
 * @param <P> - Query Parameter
 */
public interface ApiClient<T, P> {
    default T execute(P p) {
        return null;
    }

    default void executeOnly(P p) {
    }
}
