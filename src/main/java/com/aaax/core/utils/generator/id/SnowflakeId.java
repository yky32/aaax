package com.aaax.core.utils.generator.id;

import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@IdGeneratorType(SnowflakeIdGenerator.class)
@Retention(RUNTIME)
@Target({FIELD, METHOD})
public @interface SnowflakeId {
}
