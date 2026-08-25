package com.aaax.core.constant.regex;

public interface SystemRegex {

    Regex EMAIL_PATTERN = new Regex("EMAIL_PATTERN", "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");
    Regex PHONE_WITH_AREA_CODE_PATTERN = new Regex("PHONE_WITH_AREA_CODE_PATTERN", "^\\d{1,3}-\\d{4,12}$");
    Regex IS_DIGIT = new Regex("IS_DIGIT", "\\d+");
    Regex IS_LETTER = new Regex("IS_LETTER", "^[a-zA-Z]+$");
    Regex IS_CAP_LETTER = new Regex("IS_LETTER", "^[A-Z]+$");
    Regex IS_SMALL_LETTER = new Regex("IS_LETTER", "^[a-z]+$");
    Regex IS_URL = new Regex("IS_URL", "^(https?|http)://[^\\s/$.?#].[^\\s]*$");
    Regex NON_NULL_NON_EMPTY = new Regex("NON_NULL_NON_EMPTY", "^\\S+$");
    Regex IS_IP = new Regex("IS_IP", "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
}