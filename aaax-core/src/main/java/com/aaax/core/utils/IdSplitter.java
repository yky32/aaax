package com.aaax.core.utils;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;

public class IdSplitter {

    /**
     * pick the last one
     * @param idString - target string
     * @return string
     */
    public static String split(String idString) {
        String[] s = idString.split("_");
        if (s.length > 1) {
            return split(s[s.length - 1]);
        } else {
            return idString;
        }
    }

    public static String split(Long idField) {
        return split(String.valueOf(idField));
    }

    public static String split(Object idField) {
        return split(String.valueOf(idField));
    }

    public static Long splitToLong(Object idField) {
        return splitToLong(split(idField));
    }

    /**
     * Parse numeric id (after {@link #split}). Invalid tokens (e.g. path segment {@code my-media})
     * become {@link BizException} {@code PAM0400} instead of uncaught {@link NumberFormatException} → SYS9999.
     */
    public static Long splitToLong(String idField) {
        try {
            return Long.valueOf(split(idField));
        } catch (NumberFormatException | NullPointerException ex) {
            throw new BizException(SystemResponse.PAM0400, "Invalid id [" + idField + "].");
        }
    }

    public static String split(String idString, int index) {
        String[] s = idString.split("_");
        if (s.length > 1) {
            return s[index - 1];
        } else {
            return idString;
        }
    }

    public static String removeNonDigit_inSuffix(String idString){
        // Remove the suffix (trailing non-digit characters) but keep the rest of the string
        return idString.replaceAll("\\D+$", "");
    }

    public static String removeNonDigit_inPrefix(String idString){
        // Remove the prefix (first non-digit characters) but keep the rest of the string
        return idString.replaceFirst("^\\D+", "");
    }

    public static String digitOnly(String idString){
        return idString.replaceAll("\\D+", "");
    }
}
