package com.homefin.application.finance.validation;

import com.homefin.application.finance.dto.CreateApplicationRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FinanceRatioValidator implements ConstraintValidator<ValidFinanceRatio, CreateApplicationRequest> {

    private BigDecimal maxRatio;

    @Override
    public void initialize(ValidFinanceRatio annotation) {
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
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(ctx.getDefaultConstraintMessageTemplate())
                .addPropertyNode("financeAmount")
                .addConstraintViolation();
        return false;
    }
}
