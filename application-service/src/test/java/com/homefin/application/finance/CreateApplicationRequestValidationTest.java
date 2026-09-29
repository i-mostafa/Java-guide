package com.homefin.application.finance;

import com.homefin.application.finance.dto.CreateApplicationRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests Bean Validation rules directly with a Validator - no Spring context needed. */
class CreateApplicationRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validRequestPasses() {
        assertThat(validator.validate(request("1500000", "1000000", 300))).isEmpty();
    }

    @Test
    void financeAbove80PercentFailsOnFinanceAmountField() {
        Set<ConstraintViolation<CreateApplicationRequest>> violations =
                validator.validate(request("1000000", "900000", 300));

        assertThat(violations).singleElement()
                .satisfies(v -> assertThat(v.getPropertyPath().toString()).isEqualTo("financeAmount"));
    }

    @Test
    void tenureOutOfRangeFails() {
        assertThat(validator.validate(request("1500000", "1000000", 400)))
                .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo("tenureMonths"));
    }

    private static CreateApplicationRequest request(String value, String finance, int tenure) {
        return new CreateApplicationRequest("DXB-MARINA-1204", "Dubai", PropertyType.APARTMENT,
                new BigDecimal(value), new BigDecimal(finance), tenure);
    }
}
