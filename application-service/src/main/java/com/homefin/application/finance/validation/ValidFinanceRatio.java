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
 *
 * <p>Role: this file DEFINES a new annotation, used as {@code @ValidFinanceRatio(maxRatio = 0.80)} on
 * CreateApplicationRequest. The annotation itself holds no logic; FinanceRatioValidator does the check.
 * TS analogy: writing your own class-validator decorator with {@code registerDecorator(...)}, or a reusable
 * zod {@code .refine()} helper.
 *
 * <p>The meta-annotations below (annotations on an annotation) tell the compiler and runtime how it behaves.
 */
// @Documented (compile time): include this annotation in generated Javadoc of classes that use it.
@Documented
// @Constraint (runtime, Bean Validation): marks this as a validation constraint and names its validator class.
@Constraint(validatedBy = FinanceRatioValidator.class)
// @Target(TYPE) (compile time): may only be placed on classes/records, not on fields or methods.
@Target(ElementType.TYPE)
// @Retention(RUNTIME): keep the annotation in the .class file AND visible via reflection at runtime, otherwise
// the validator could never see it. (Annotations are compile-time only unless retained.)
@Retention(RetentionPolicy.RUNTIME)
// "@interface" = the keyword for declaring an annotation type (not an interface you implement).
public @interface ValidFinanceRatio {

    // Annotation "attributes" are declared like methods; "default" gives the value when omitted.
    double maxRatio() default 0.80;

    // The next three attributes are REQUIRED by the Bean Validation spec for every constraint.
    // {maxRatio} in the message is replaced with the attribute value when the error is built.
    String message() default "financeAmount must not exceed {maxRatio} of propertyValue";

    // Class<?>[] = array of Class objects of any type. "?" is a generic wildcard (unknown type), like
    // `Class<unknown>` in TS. groups = run this constraint only in certain validation groups; {} = array literal.
    Class<?>[] groups() default {};

    // "? extends Payload" = a bounded wildcard: any class that is Payload or a subtype of it.
    Class<? extends Payload>[] payload() default {};
}
