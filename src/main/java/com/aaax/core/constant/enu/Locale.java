package com.aaax.core.constant.enu;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum Locale {
    EN("en", "en"),
    zh_CN("zh-CN", "cn"),
    zh_TW("zh-TW", "zh"),
    JP("ja", "ja"),
    TH("th", "th"),
    KO("ko", "ko"),
    VI("vi", "vi"),
    ID("id", "id");

    private final String code;
    private final String i18nCode;

    Locale(String code, String i18nCode) {
        this.code = code;
        this.i18nCode = i18nCode;
    }

    public static Locale get(String input) {
        for (Locale value : Locale.values()) {
            if (input.equalsIgnoreCase(value.name()) ||
                    input.equalsIgnoreCase(value.getCode()) ||
                    input.equalsIgnoreCase(value.getI18nCode())) {
                return value;
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(Locale.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
