package com.aaax.core.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ TYPE, FIELD, ANNOTATION_TYPE })
@Retention(RUNTIME)
public @interface PasswordFormat {

    String message() default "Password must be at least 8 characters and include 1 capital letter and 1 symbol or number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
