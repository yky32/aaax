package com.aaax.core.constant.enu;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;
import com.aaax.core.utils.StringUtil;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

@Getter
public enum CardBrand {
    OTHER_BRAND("OB", List.of(), List.of()),
    VISA("VISA", List.of("^4[0-9]{12}(?:[0-9]{3})?$"), List.of()),
    MASTERCARD("MC",
            List.of("^(5[1-5][0-9]{14}|222[1-9][0-9]{12}|22[3-9][0-9]{13}|2[3-6][0-9]{14}|27[0-1][0-9]{13}|2720[0-9]{12})$"), List.of()
    ),
    AMERICAN_EXPRESS("AE", List.of("^3[47][0-9]{13}$"), List.of()),
    DISCOVER("DIS", List.of("^6(?:011|5[0-9]{2})[0-9]{12}$"), List.of()),
    DISCOVER_DINERS("DISD", List.of("^3[0689][0-9]{12}[0-9]*$"), List.of()),
    JCB("JCB", List.of("^35[0-9]{14,17}$", "^(?:2131|1800|35\\d{3})\\d{11}$"), List.of()),
    CHINA_UNIONPAY("UNI", List.of("^(62[0-9]{14,17})$"), List.of()),
    INTERAC("INT", List.of(), List.of()),
    EFTPOS("EFT", List.of(), List.of()),
    FELICA("FEL", List.of(), List.of()),
    EBT("EBT", List.of(), List.of()),
    ALL("ALL", List.of(), List.of())
    ;
    private static final Logger log = LoggerFactory.getLogger(CardBrand.class);

    private final String shortForm;
    private final List<String> patterns;
    private final List<String> prefixPatterns;

    CardBrand(String shortForm, List<String> patterns, List<String> prefixPatterns) {
        this.shortForm = shortForm;
        this.patterns = patterns;
        this.prefixPatterns = prefixPatterns;
    }

    public static CardBrand classify(String input) {
        for (CardBrand value : CardBrand.values()) {
            for (String pattern : value.patterns) {
                if (input.matches(pattern)){
                    return value;
                }
            }
        }
        String pan = StringUtil.maskString(input, 6, 12, '*');
        String message = String.format("Wrong CCN [%s] value. [%s] not in-> %s", pan, pan, Arrays.asList(CardBrand.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }

    public static CardBrand classifyFirstSix(String input) {
        if (input.length() < 6) {
            String message = String.format("Input must be at least 6 characters long [%s]", 6);
            throw new BizException(SystemResponse.PAM0400, message);
        }

        for (CardBrand value : CardBrand.values()) {
            for (String pattern : value.prefixPatterns) {
                if (input.substring(0, 6).matches(pattern)){
                    return value;
                }
            }
        }

        String message = String.format("No matching card brand for first [%s] digits", input);
        throw new BizException(SystemResponse.PAM0400, message);
    }

    public static CardBrand get(String input) {
        for (CardBrand value : CardBrand.values()) {
            if (input.equalsIgnoreCase(value.name())){
                return value;
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in-> %s", input, input, Arrays.asList(CardBrand.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
