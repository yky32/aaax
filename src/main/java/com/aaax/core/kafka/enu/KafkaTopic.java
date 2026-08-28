package com.aaax.core.kafka.enu;

public interface KafkaTopic {

    // ==================== [notification service]
    String NOTIFICATION_OTP_GENERATED = "notification.otp.generated";
    String NOTIFICATION_TRANSACTION_DONE = "notification.transaction.done";
    String NOTIFICATION_STRESS_TEST = "notification.stress-test";
    String NOTIFICATION_STANDARD = "notification.standard";
    String NOTIFICATION_MASS = "notification.mass";
    String NOTIFICATION_CRON_CHECKER = "notification.cron-checker";
    String NOTIFICATION_MARK_IS_READ = "notification.mark-is-read";
    String NOTIFICATION_MARK_IS_ARCHIVED = "notification.mark-is-archived";
    String NOTIFICATION_MASS_RECONCILIATION = "notification.mass-reconciliation";
    String NOTIFICATION_LOG_CREATED = "notification.log-created";
    String NOTIFICATION_USER_LOG_UPDATED = "notification.user-log-updated";
    // ==================== [notification service]

    // ==================== [aaax service]
    String USER_AUTH_FORCED_LOGOUT = "user.auth.forced-logout";
    String USER_CREATED = "user.created";
    String USER_DELETED = "user.deleted";
    String USER_LOG = "user.log";
    String USER_HOUSEKEEPING_EXPIRED_USER_TOKENS = "user.operations.housekeeping-expired-user-tokens";
    String USER_STATE_CHANGED = "user.state.changed";
    String USER_ALIAS_GENERATED = "user.alias.generated";// stat
    String USER_USER_PROFILE_GENERATED = "user.user-profile.generated";// stat// us, password.
    String USER_USER_ROUTES_CREATED = "user.user-routes.created";
    String USER_USER_PERMISSION_MUTATED = "user.user-permission.mutated";
    String USER_USER_STATUS_MUTATED = "user.user-status.mutated";
    String USER_LOGIN_ATTEMPTS_MUTATED = "user.login-attempts.mutated";
    String USER_POST_LOGIN_SUCCEED = "user.post-login.succeed";
    // ==================== [aaax service]

    // ==================== [log service]
    String ACTIVITY_LOG_CREATED = "activity-log.created";
    String ACTIVITY_LOG_NOTIFICATION_SENT = "activity-log.notification.sent";
    // ==================== [log service]

    // ==================== [util service]
    String UTIL_CDN_FILE_DELETE = "util.cdn-file.deleted";
    // ==================== [util service]
}
