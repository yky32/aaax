package com.aaax.core.redis;

public enum CoreRedisKey {
    // ==== user ====
    SYSTEM_IP("system:", "ip:"),
    SYSTEM_FEATURE_FLAG("system:", "feature-flag:"),
    SYSTEM_CLIENT_SECRET_GRANT("system:", "client-secret-grant-token:")
    // ==== user end ====
    ;

    private final String domain;
    private final String feature;

    CoreRedisKey(String domain, String feature) {
        this.domain = domain;
        this.feature = feature;
    }

    public String getKey() {
        return this.domain.concat(this.feature);
    }
 }
