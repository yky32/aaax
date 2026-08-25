package com.aaax.server.config.aop.mask;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MaskField {
    MaskType type();
    int numDigitsToMask() default 3;

    char symbol() default '*';
}
