package com.homefin.application.finance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

// Static imports: call assertThat / assertThatThrownBy without the "Assertions." prefix.
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests of the annuity maths in InstallmentCalculator (plain JUnit, no Spring, no mocks).
 *
 * <p>TS analogy: jest {@code it.each} table tests for a pure function.
 */
class InstallmentCalculatorTest {

    private final InstallmentCalculator calculator = new InstallmentCalculator();

    // @ParameterizedTest (runtime, JUnit): run this method once per row of input. name = display name of each
    // run, where {0}, {1}... are the arguments. Like it.each(table)('%s at %s over %s months = %s', ...).
    // @CsvSource: the rows, one CSV string each; JUnit converts every column to the parameter's type
    // (String, int...). The array literal { "...", "..." } is the annotation's value.
    @ParameterizedTest(name = "{0} at {1} over {2} months = {3}")
    @CsvSource({
            "1000000, 0.05, 300, 5845.90",
            "120000,  0.00, 12,  10000.00"
    })
    void computesAnnuityInstallment(String principal, String rate, int months, String expected) {
        BigDecimal result = calculator.monthlyInstallment(new BigDecimal(principal), new BigDecimal(rate), months);
        // compareTo, not equals: BigDecimal.equals also compares scale (2.0 != 2.00)
        assertThat(result).isEqualByComparingTo(expected);
    }

    // @Test (runtime, JUnit): a single test case.
    @Test
    void rejectsNonPositiveTenure() {
        // BigDecimal.TEN / BigDecimal.ONE are predefined constants.
        assertThatThrownBy(() -> calculator.monthlyInstallment(BigDecimal.TEN, BigDecimal.ONE, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
