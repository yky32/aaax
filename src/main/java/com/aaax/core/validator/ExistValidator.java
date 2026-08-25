package com.aaax.core.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ExistValidator implements ConstraintValidator<Exist, Object> {
    @Autowired
    private ApplicationContext applicationContext;

    private FieldValueExists service;
    private String fieldName;

    @Override
    public void initialize(Exist unique) {
        Class<? extends FieldValueExists> clazz = unique.service();
        this.fieldName = unique.fieldName();

        String serviceQualifier = unique.serviceQualifier();
        if (!serviceQualifier.trim().equals("")) {
            this.service = this.applicationContext.getBean(serviceQualifier, clazz);
        } else {
            this.service = this.applicationContext.getBean(clazz);
        }
    }

    @Override
    public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
        return this.service.fieldValueExists(o, this.fieldName);
    }
}