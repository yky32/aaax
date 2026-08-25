package com.aaax.core.utils;

import com.aaax.core.common.AppContext;
import com.aaax.core.common.AppContextHolder;
import com.aaax.core.constant.regex.Regex;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.Response;
import com.aaax.core.response.SystemResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
public class ValidationUtil {

    public static void nonEmptyNonNull(Object field, String name, Regex regex, Map<String, Object> customMessage) {
        customMessage.put("regex", regex.getDescription());
        nonEmptyNonNull(field, name, regex.getPattern(), customMessage);
    }
    public static void nonEmptyNonNull(Object field, String name, String regexPattern, Map<String, Object> customMessage) {
        customMessage.put("field", name);
        try {
            AppContext appContext = AppContextHolder.CONTEXT.get();
            if (appContext != null) {
                customMessage.put("url", appContext.getRequestContext().getApiUrl());
            }
        } catch (Exception exception) {
            log.info("-- Error in get [AppContext]..");
        }
        nonEmptyNonNull(field, regexPattern, customMessage);
    }

    public static void nonEmptyNonNull_regexps(Object field, String name, List<String> regexPatterns, Map<String, Object> customMessage) {
        customMessage.put("field", name);
        try {
            AppContext appContext = AppContextHolder.CONTEXT.get();
            if (appContext != null) {
                customMessage.put("url", appContext.getRequestContext().getApiUrl());
            }
        } catch (Exception exception) {
            log.info("-- Error in get [AppContext]..");
        }

        int counter = 0;
        boolean isValid = false;
        while (counter <= regexPatterns.size() && !isValid){
            try {
                String validationCriteria = regexPatterns.get(counter);
                List<String> details = (List<String>) customMessage.getOrDefault("details", new ArrayList<>());
                details.add(validationCriteria);
                customMessage.put("details", details);
                nonEmptyNonNull(field, validationCriteria, customMessage);
                isValid = true; // quick return.
            } catch (Exception exception) {
                counter ++;
            }
        }
        // final result.
        if (!isValid) {
            throw new BizException(SystemResponse.PAM0400, customMessage);
        }
    }

    public static void nonEmptyNonNull(Object field, String name, List<String> checkingValues, Map<String, Object> customMessage) {
        customMessage.put("field", name);
        customMessage.put("acceptedOptions", checkingValues);
        try {
            AppContext appContext = AppContextHolder.CONTEXT.get();
            if (appContext != null) {
                customMessage.put("url", appContext.getRequestContext().getApiUrl());
            }
        } catch (Exception exception) {
            log.info("-- Error in get [AppContext]..");
        }
        nonEmptyNonNull(field, checkingValues, customMessage);
    }

    public static void nonEmptyNonNull(Object field, String name, List<String> checkingValues) {
        String message = String.format("[%s] must [nonEmptyNonNull]", name);
        nonEmptyNonNull(field, checkingValues, message);
    }

    public static void nonEmptyNonNull(Object field, String name) {
        String message = String.format("[%s] must [nonEmptyNonNull]", name);
        if (StringUtils.isBlank(String.valueOf(field))) throw new BizException(SystemResponse.PAM0400, message);
        Optional.ofNullable(field).orElseThrow(() -> new BizException(SystemResponse.PAM0400, message));
    }

    private static void nonEmptyNonNull(Object field, Object validationCriteria, Object message) {
        if (StringUtils.isBlank(String.valueOf(field))) throw new BizException(SystemResponse.PAM0400, message);
        Optional.ofNullable(field).orElseThrow(() -> new BizException(SystemResponse.PAM0400, message));

        // setting
        boolean isValid = true;
        Object finalMessage = null;

        if (validationCriteria instanceof String) {
            isValid = patternMatches(String.valueOf(field), (String) validationCriteria);
        } else if (validationCriteria instanceof List<?>) {
            isValid = ((List<?>) validationCriteria).contains(String.valueOf(field));
        }

        if (message instanceof String) {
            if (validationCriteria instanceof String) {
                finalMessage = ((String) message).concat(". Mismatch regexPattern");
            } else if (validationCriteria instanceof List<?>) {
                finalMessage = ((String) message).concat(". Mismatch checkingValues");
            } else {
                finalMessage = message;
            }
        } else if (message instanceof Map) {
            finalMessage = message;
        }

        // final result.
        if (!isValid) {
            throw new BizException(SystemResponse.PAM0400, finalMessage);
        }
    }

    public static boolean patternMatches(String target, String regexPattern) {
        return Pattern.compile(regexPattern).matcher(target).matches();
    }

    public static void patternMatches(String target, String regexPattern, Response response) {
        Map<String, String> detail = Map.of("detail", regexPattern);
        patternMatches(target, regexPattern, response, detail);
    }

    public static void patternMatches(String target, String regexPattern, Response response, Map map) {
        boolean isValid = patternMatches(target, regexPattern);
        if (!isValid) {
            throw new BizException(response, map);
        }
    }

    public static boolean isValidCreditCardNumber(String creditCardNumber) {
        int sum = 0;
        boolean alternate = false;
        for (int i = creditCardNumber.length() - 1; i >= 0; i--) {
            int digit = Integer.parseInt(creditCardNumber.substring(i, i + 1));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}
