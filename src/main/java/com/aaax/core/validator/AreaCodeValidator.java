package com.aaax.core.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AreaCodeValidator implements ConstraintValidator<AreaCodeFormat, String> {

    private static final Pattern PATTERN = Pattern.compile("^\\d{1,3}$");

    @Override
    public boolean isValid(final String areaCode, ConstraintValidatorContext constraintValidatorContext) {
        if (areaCode == null || areaCode.isEmpty()) {
            return true;
        }
        return validateAreaCode(areaCode);
    }

    private boolean validateAreaCode(final String areaCode) {
        Matcher matcher = PATTERN.matcher(areaCode);
        return matcher.matches();
    }
}
