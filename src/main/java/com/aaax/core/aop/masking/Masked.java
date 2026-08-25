package com.aaax.core.aop.masking;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Masked {
    char symbol() default '*'; // Default masking symbol
    int start() default 0;       // Start index for masking
    int end() default 0;         // End index for masking
    String[] fields() default {};
}
