package com.aaax.config.redis;

import lombok.Getter;

@Getter
public enum RedisKey {
    // ==== user ====
    USER_OAUTH_TOKENS("user:", "tokens:"),
    USER_OAUTH_TOKENS_REFRESH_TOKEN("user:", "refresh-token:"),
    USER_WS_HASH("user:", "ws-hash:"),
    USER_AUTH_SESSIONS("user:", "auth-sessions:"),
    USER_AUTH_SESSIONS_COUNT("user:", "auth-sessions-count:"),
    USER_CLIENT_CREDENTIALS("user:", "client-credentials:"),
    USER_OAUTH_TOKENS_REFRESH_TOKEN_HISTORY("user:", "refresh-token-history:"),
    // ==== user end ====

    // ==== extra login usecase =====
    LOGIN_MY_TENANTS("user:", "login-my-tenants:"),
    LOGIN_MY_ROUTES("user:", "login-my-routes:"),
    LOGIN_MY_PERMISSIONS("user:", "login-my-permissions:"),
    LOGIN_MY_ROLES("user:", "login-my-roles:"),
    // ==== extra login usecase =====

    // ==== OTP =====
    OTP_GENERAL("otp:", "general:"),
    OTP_USER_REGISTER("otp:", "user-register:"),
    OTP_RESET_PASSWORD("otp:", "reset-password:"),
    OTP_CUSTOM("otp:", "custom:"),
    // ==== OTP =====

    // ==== SYSTEM CONFIGURATION ====
    UAA_SYSTEM_CONFIGURATION("uaa:", "system-configuration:"),

    // ==== Others =====
    DEVICE_SESSIONS("device:", "session:"),
    ;

    private final String domain;
    private final String feature;

    RedisKey(String domain, String feature) {
        this.domain = domain;
        this.feature = feature;
    }

    public String getKey() {
        return this.domain.concat(this.feature);
    }
 }
