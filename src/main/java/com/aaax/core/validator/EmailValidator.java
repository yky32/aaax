package com.aaax.core.validator;

import com.aaax.core.constant.RegexPatternConstant;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailValidator implements ConstraintValidator<EmailFormat, String> {

    private static final Pattern PATTERN = Pattern.compile(RegexPatternConstant.EMAIL_PATTERN);

    @Override
    public boolean isValid(final String email, ConstraintValidatorContext constraintValidatorContext) {
        if (email == null || email.isEmpty()) {
            return true;
        }
        return validateEmail(email);
    }

    private boolean validateEmail(final String email) {
        Matcher matcher = PATTERN.matcher(email);
        return matcher.matches();
    }
}
