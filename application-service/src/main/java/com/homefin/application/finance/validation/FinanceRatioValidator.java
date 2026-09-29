package com.homefin.application.finance.validation;

import com.homefin.application.finance.dto.CreateApplicationRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The logic behind the {@code @ValidFinanceRatio} annotation.
 *
 * <p>Role: Bean Validation creates an instance of this class (it is linked via
 * {@code @Constraint(validatedBy = FinanceRatioValidator.class)}), calls {@code initialize} once with the
 * annotation instance, then {@code isValid} for every CreateApplicationRequest being validated.
 * It is not a Spring bean, so it has no {@code @Component}. TS analogy: the predicate function passed to zod's
 * {@code .refine((r) => r.financeAmount / r.propertyValue <= 0.8, { path: ['financeAmount'] })}.
 */
// implements = this class fulfils an interface contract (like `class X implements Y` in TS).
// ConstraintValidator<A, T> is generic: A = the annotation type, T = the type of value being validated.
public class FinanceRatioValidator implements ConstraintValidator<ValidFinanceRatio, CreateApplicationRequest> {

    // Mutable instance field (no final): set in initialize(), read in isValid().
    private BigDecimal maxRatio;

    // @Override (compile time, javac): asserts this method implements/overrides one from the interface or
    // superclass; compilation fails if the signature doesn't match. Like TS 4.3+ `override`.
    @Override
    public void initialize(ValidFinanceRatio annotation) {
        // Annotation attributes are read like methods: annotation.maxRatio(). BigDecimal.valueOf(double)
        // uses the double's shortest decimal string (0.8 -> "0.8"), avoiding binary noise from new BigDecimal(0.8).
        this.maxRatio = BigDecimal.valueOf(annotation.maxRatio());
    }

    @Override
    public boolean isValid(CreateApplicationRequest r, ConstraintValidatorContext ctx) {
        if (r == null || r.financeAmount() == null || r.propertyValue() == null
                || r.propertyValue().signum() <= 0) {
            return true; // field-level constraints report those problems
        }
        BigDecimal ratio = r.financeAmount().divide(r.propertyValue(), 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(maxRatio) <= 0) {
            return true;
        }
        // Attach the error to a specific field instead of the object -> nicer client error.
        // Replace the default (class-level) violation with one pointing at "financeAmount", reusing the
        // annotation's message template ("{maxRatio}" in it is interpolated with the attribute value).
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(ctx.getDefaultConstraintMessageTemplate())
                .addPropertyNode("financeAmount")
                .addConstraintViolation();
        return false;
    }
}
