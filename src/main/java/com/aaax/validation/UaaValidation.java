package com.aaax.validation;

import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;
import com.aaax.core.utils.ValidationUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UaaValidation {

    private static final String mobileRegexp = "^\\d{8}";
    private static final String emailRegexp = "^(.+)@(\\S+)$";

    public final static Map<LoginType, String> LOGIN_TYPE_REGEX_CHECK = Map.of(
            // LoginType, ___ value
            LoginType.MOBILE, mobileRegexp,
            LoginType.OTP, mobileRegexp,
            LoginType.EMAIL, emailRegexp
    );


    @NotNull
    public static LoginType detechLoginType(String username) {
        // === check valid strings
        if (username != null && username.matches(".*" + "[\u4e00-\u9fa5]" + ".*")) {
            throw new BizException(SystemResponse.PAM0400, "Input contains Chinese characters. =>".concat(username));
        }
        if (username == null) {
            throw new BizException(SystemResponse.PAM0400, "username was null");
        }
        long count = username.chars().filter(ch -> ch == '@').count();
        if (count > 1) {
            throw new BizException(SystemResponse.PAM0400, "Input contains more than two '@' characters. =>".concat(username));
        }

        LoginType loginType;
        if (ValidationUtil.patternMatches(username,
                UaaValidation.LOGIN_TYPE_REGEX_CHECK.get(LoginType.MOBILE))) {
            loginType = LoginType.MOBILE;
        } else if (ValidationUtil.patternMatches(username,
                UaaValidation.LOGIN_TYPE_REGEX_CHECK.get(LoginType.EMAIL))) {
            loginType = LoginType.EMAIL;
        } else {
            loginType = LoginType.USERNAME;
        }
        return loginType;
    }

    /**
     * Canonical login identifier for storage, lookup, OTP Redis keys, and login.
     * <ul>
     *   <li>EMAIL / USERNAME → trim + {@link Locale#ROOT} lower-case (case-insensitive identity)</li>
     *   <li>MOBILE / OTP → trim only (digit shape unchanged)</li>
     *   <li>Other login types (social ids etc.) → lower-case for consistency</li>
     * </ul>
     */
    public static String toCanonicalIdentifier(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        LoginType type = detechLoginType(trimmed);
        if (type == LoginType.MOBILE || type == LoginType.OTP) {
            return trimmed;
        }
        // EMAIL, USERNAME, social-ish strings used as identifiers
        return trimmed.toLowerCase(Locale.ROOT);
    }

    /** Null-safe canonical identifier; blank stays blank. */
    public static String toCanonicalIdentifierIfPresent(String raw) {
        if (StringUtils.isBlank(raw)) {
            return raw;
        }
        return toCanonicalIdentifier(raw);
    }

    public static String check_passwordRequirement(PasswordEncoder passwordEncoder, String credentials, List<String> regexps) {
        // ___ FIXME: check password regular exp
        for (String regexp : regexps) {
            if (!ValidationUtil.patternMatches(credentials, regexp)) {
                throw new BizException(SystemResponse.PAM0400, "Invalid Password Requirement. =>".concat(regexp));
            }
        }
        return passwordEncoder.encode(credentials);
    }
}
