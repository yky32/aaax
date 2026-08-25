package com.aaax.core.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<PasswordFormat, String> {

    private static final Pattern PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[`~!@#$%^&*()\\-_=+\\[\\]{}\\\\|;:,.<>/?])[A-Za-z\\d`~!@#$%^&*()\\-_=+\\[\\]{}\\\\|;:,.<>/?]{8,}$");

    @Override
    public boolean isValid(final String password, ConstraintValidatorContext constraintValidatorContext) {
        if (password == null || password.isEmpty()) {
            return true;
        }
        return validatePassword(password);
    }

    private boolean validatePassword(final String password) {
        Matcher matcher = PATTERN.matcher(password);
        return matcher.matches();
    }
}
