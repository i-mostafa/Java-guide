package com.homefin.customer.customer.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
// Period = a date-based amount of time (years, months, days), as opposed to Duration (hours/minutes/seconds).
import java.time.Period;

/**
 * The logic behind the {@link Adult} annotation. Bean Validation (Hibernate Validator) instantiates this class and
 * calls {@code isValid} whenever it validates a field annotated with {@code @Adult}, e.g. when a controller receives
 * a {@code @Valid} request body. Like a custom {@code .refine()} check in a zod schema, or a class-validator
 * {@code @ValidatorConstraint} class.
 *
 * <p>{@code implements ConstraintValidator<Adult, LocalDate>}: this class fulfils the ConstraintValidator interface
 * contract (like {@code implements} in TS). The generics say "validator for the Adult annotation, applied to values
 * of type LocalDate".
 */
public class AdultValidator implements ConstraintValidator<Adult, LocalDate> {

    // "private" = only visible inside this class (like a TS #private field, enforced by the compiler/JVM).
    // "int" = primitive 32-bit integer, defaults to 0. Not final because it's set in initialize(), not a constructor.
    private int minAge;

    // Called once per annotated field with the annotation instance, so we can read its attributes (minAge = 18...).
    // @Override (compile time): the compiler checks this really implements a method from ConstraintValidator.
    @Override
    public void initialize(Adult annotation) {
        // "this.minAge" = the field of this instance, same meaning as "this" in TS. Annotation attributes are
        // read like methods: annotation.minAge().
        this.minAge = annotation.minAge();
    }

    // Called for every value being validated. Return true = valid, false = constraint violation (-> HTTP 400).
    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null-ness is @NotNull's job - keep constraints single-purpose
        }
        // Whole years between the birth date and today (calendar-aware, handles leap years).
        return Period.between(value, LocalDate.now()).getYears() >= minAge;
    }
}
