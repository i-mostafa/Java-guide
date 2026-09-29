package com.homefin.application.finance;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Standard annuity formula:  P * r * (1+r)^n / ((1+r)^n - 1),  r = annual rate / 12.
 * (In Islamic finance the "profit rate" replaces interest; the installment maths is the same.)
 */
@Component
public class InstallmentCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    public BigDecimal monthlyInstallment(BigDecimal principal, BigDecimal annualRate, int months) {
        if (months <= 0) {
            throw new IllegalArgumentException("months must be positive");
        }
        if (annualRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_EVEN);
        }
        BigDecimal r = annualRate.divide(TWELVE, MC);
        BigDecimal growth = BigDecimal.ONE.add(r, MC).pow(months, MC);
        return principal.multiply(r, MC)
                .multiply(growth, MC)
                .divide(growth.subtract(BigDecimal.ONE, MC), MC)
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}
