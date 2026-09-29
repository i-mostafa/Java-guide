package com.homefin.application.finance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstallmentCalculatorTest {

    private final InstallmentCalculator calculator = new InstallmentCalculator();

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

    @Test
    void rejectsNonPositiveTenure() {
        assertThatThrownBy(() -> calculator.monthlyInstallment(BigDecimal.TEN, BigDecimal.ONE, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
