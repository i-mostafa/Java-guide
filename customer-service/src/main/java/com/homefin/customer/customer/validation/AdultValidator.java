package com.homefin.customer.customer.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

public class AdultValidator implements ConstraintValidator<Adult, LocalDate> {

    private int minAge;

    @Override
    public void initialize(Adult annotation) {
        this.minAge = annotation.minAge();
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null-ness is @NotNull's job - keep constraints single-purpose
        }
        return Period.between(value, LocalDate.now()).getYears() >= minAge;
    }
}
