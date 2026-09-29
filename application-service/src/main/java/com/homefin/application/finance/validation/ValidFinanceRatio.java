package com.homefin.application.finance.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CLASS-level (cross-field) constraint: financeAmount / propertyValue must not exceed maxRatio.
 * Use class-level constraints whenever a rule involves more than one field.
 */
@Documented
@Constraint(validatedBy = FinanceRatioValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFinanceRatio {

    double maxRatio() default 0.80;

    String message() default "financeAmount must not exceed {maxRatio} of propertyValue";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
