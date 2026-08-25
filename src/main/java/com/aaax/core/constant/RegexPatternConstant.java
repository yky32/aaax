package com.aaax.core.constant;

public class RegexPatternConstant {

    public static String EMAIL_PATTERN = "[A-Za-z0-9\\._%+\\-]+@[A-Za-z0-9\\.\\-]+\\.[A-Za-z]{2,}";
    public static String PHONE_WITH_AREA_CODE_PATTERN = "^\\d{1,3}-\\d{4,12}$";
    public static String IS_VALID_CC_EXP_YEAR = "^(2[4-9]|[3-9][0-9])$";
    public static String IS_VALID_CC_EXP_MONTH = "^(0[1-9]|1[0-2])$";
    public static String IS_VALID_CVV = "^[0-9]{3,4}$";
    public static String IS_DIGIT = "\\d+";
    public static String IS_HTTPS_URL = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
    public static String IS_CHINESE = "[\u4e00-\u9fa5]";
    public static String IS_ENGLISH = "^[A-Za-z]+$";
    public static String IS_ENGLISH_WITH_SPACE = "^[A-Za-z ]+$";
}
