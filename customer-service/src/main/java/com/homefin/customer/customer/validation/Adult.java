package com.homefin.customer.customer.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Custom field-level constraint: the date must be at least {@code minAge} years ago. */
@Documented
@Constraint(validatedBy = AdultValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Adult {

    int minAge() default 18;

    String message() default "customer must be at least {minAge} years old";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
