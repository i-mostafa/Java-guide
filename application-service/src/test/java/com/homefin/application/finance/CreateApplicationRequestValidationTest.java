package com.homefin.application.finance;

import com.homefin.application.finance.dto.CreateApplicationRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

// "import static" imports a static METHOD so it can be called without its class name:
// assertThat(x) instead of Assertions.assertThat(x). Like `import { expect } from '@jest/globals'`.
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests Bean Validation rules directly with a Validator - no Spring context needed.
 *
 * <p>Role: fast unit tests of the annotations on CreateApplicationRequest, including the custom
 * {@code @ValidFinanceRatio}. TS analogy: {@code expect(schema.safeParse(input).success).toBe(true)} in jest.
 * The test runner is JUnit 5 (run by Maven Surefire on {@code mvn test}); assertions use AssertJ's fluent
 * {@code assertThat(actual).isXxx()} style, similar to jest's {@code expect(actual).toXxx()}.
 */
// Test classes and methods don't need to be public in JUnit 5 (package-private is the convention).
class CreateApplicationRequestValidationTest {

    // A field initialized once per test (JUnit creates a NEW instance of the class for every test method,
    // so fields are fresh each time, like a beforeEach that rebuilds everything).
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    // @Test (runtime, JUnit): marks a test case, like `it('valid request passes', () => {...})`.
    @Test
    void validRequestPasses() {
        // validate(...) returns the set of violations; empty = valid.
        assertThat(validator.validate(request("1500000", "1000000", 300))).isEmpty();
    }

    @Test
    void financeAbove80PercentFailsOnFinanceAmountField() {
        // Set<ConstraintViolation<CreateApplicationRequest>>: nested generics - a set of violations of that type.
        Set<ConstraintViolation<CreateApplicationRequest>> violations =
                validator.validate(request("1000000", "900000", 300));

        // singleElement(): assert exactly one element, then continue asserting on it.
        // satisfies(v -> ...): run nested assertions on that element (lambda parameter v).
        assertThat(violations).singleElement()
                .satisfies(v -> assertThat(v.getPropertyPath().toString()).isEqualTo("financeAmount"));
    }

    @Test
    void tenureOutOfRangeFails() {
        // anySatisfy: at least one element must pass the nested assertion (like expect.arrayContaining).
        assertThat(validator.validate(request("1500000", "1000000", 400)))
                .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo("tenureMonths"));
    }

    // Test-data helper. new BigDecimal("1500000") is built from a String so the value is exact.
    // The int `tenure` is auto-boxed into the record's Integer component.
    private static CreateApplicationRequest request(String value, String finance, int tenure) {
        return new CreateApplicationRequest("DXB-MARINA-1204", "Dubai", PropertyType.APARTMENT,
                new BigDecimal(value), new BigDecimal(finance), tenure);
    }
}
