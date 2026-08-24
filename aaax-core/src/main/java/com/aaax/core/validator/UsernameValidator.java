package com.aaax.core.validator;

import com.aaax.core.constant.RegexPatternConstant;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UsernameValidator implements ConstraintValidator<UsernameFormat, String> {

    private static final Pattern PATTERN = Pattern.compile("(" + RegexPatternConstant.EMAIL_PATTERN + ")|(" + RegexPatternConstant.PHONE_WITH_AREA_CODE_PATTERN +")");

    @Override
    public boolean isValid(final String username, ConstraintValidatorContext constraintValidatorContext) {
        if (username == null || username.isEmpty()) {
            return true;
        }
        return validateUsername(username);
    }

    private boolean validateUsername(final String username) {
        Matcher matcher = PATTERN.matcher(username);
        return matcher.matches();
    }
}
