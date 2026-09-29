package com.homefin.application.finance;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Standard annuity formula:  P * r * (1+r)^n / ((1+r)^n - 1),  r = annual rate / 12.
 * (In Islamic finance the "profit rate" replaces interest; the installment maths is the same.)
 *
 * <p>Role: a pure, stateless calculation used by FinanceApplicationService. It is a Spring bean so it can be
 * injected (and swapped), yet tests simply do {@code new InstallmentCalculator()}. TS analogy: an
 * {@code @Injectable()} class with no dependencies, or just an exported pure function.
 *
 * <p>Why BigDecimal and not double: double is binary floating point, so money values like 0.1 are
 * approximations and errors accumulate over 300 months. BigDecimal stores exact decimal digits; you choose
 * precision and rounding explicitly.
 */
// @Component (runtime): registered as a singleton bean by component scanning.
@Component
public class InstallmentCalculator {

    // MathContext = "how many significant digits + which rounding" for intermediate results.
    // DECIMAL64 = 16 significant digits, HALF_EVEN rounding (like IEEE decimal64).
    private static final MathContext MC = MathContext.DECIMAL64;
    // BigDecimal is an object, not a primitive: constants are created once with BigDecimal.valueOf(...).
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    // int months = primitive integer parameter (never null).
    public BigDecimal monthlyInstallment(BigDecimal principal, BigDecimal annualRate, int months) {
        if (months <= 0) {
            // IllegalArgumentException = Java's built-in "bad argument" unchecked exception
            // (like a TypeError/RangeError in JS).
            throw new IllegalArgumentException("months must be positive");
        }
        // signum() = -1, 0 or 1. A 0% rate would divide by zero in the formula, so split evenly instead.
        // HALF_EVEN ("banker's rounding"): .5 rounds to the nearest EVEN digit, avoiding systematic upward bias.
        if (annualRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_EVEN);
        }
        BigDecimal r = annualRate.divide(TWELVE, MC);
        BigDecimal growth = BigDecimal.ONE.add(r, MC).pow(months, MC);
        // BigDecimal is immutable: every operation returns a NEW value, so calls are chained.
        // Full precision until the end, then setScale(2, HALF_EVEN) rounds once to cents.
        return principal.multiply(r, MC)
                .multiply(growth, MC)
                .divide(growth.subtract(BigDecimal.ONE, MC), MC)
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}
