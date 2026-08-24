package com.aaax.core.validator;

import com.aaax.core.constant.RegexPatternConstant;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneNumberValidator implements ConstraintValidator<PhoneNumberFormat, String> {

    private static final Pattern PATTERN = Pattern.compile(RegexPatternConstant.PHONE_WITH_AREA_CODE_PATTERN);

    @Override
    public boolean isValid(final String phoneNumber, ConstraintValidatorContext constraintValidatorContext) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return true;
        }
        return validatePhoneNumber(phoneNumber);
    }

    private boolean validatePhoneNumber(final String phoneNumber) {
        Matcher matcher = PATTERN.matcher(phoneNumber);
        return matcher.matches();
    }
}
