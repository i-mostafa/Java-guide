package com.homefin.application.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Business parameters as configuration -> can differ per environment / be changed without a rebuild.
 *
 * <p>Role: a typed, validated view of the {@code app.finance.*} keys in application.yml. Spring binds
 * the values at startup (kebab-case keys like {@code annual-profit-rate} map to camelCase
 * {@code annualProfitRate}) and injects this object wherever it is needed (FinanceApplicationService).
 * If validation fails, the app refuses to start - "fail fast" on bad config.
 *
 * <p>TS analogy: {@code const financeConfig = z.object({ annualProfitRate: z.number().min(0).max(0.5), ... })
 * .parse(config.app.finance)} run once at boot, or a NestJS {@code registerAs('finance', ...)} config.
 */
// @Validated (runtime): tells Spring to run the Bean Validation annotations below when binding.
@Validated
// @ConfigurationProperties (runtime): bind every key under "app.finance" into this record.
// Discovered thanks to @ConfigurationPropertiesScan on the main class.
@ConfigurationProperties(prefix = "app.finance")
// "record" = an immutable data class. The parameter list declares private final fields, a constructor,
// accessor methods annualProfitRate() / maxFinanceToValue() (no "get" prefix), plus equals/hashCode/toString.
// TS analogy: `type FinanceProperties = Readonly<{ annualProfitRate: Decimal; maxFinanceToValue: Decimal }>`.
public record FinanceProperties(
        // Bean Validation constraints (checked at runtime by the validator when binding):
        // @NotNull = required, @DecimalMin/@DecimalMax = inclusive numeric bounds, written as strings so
        // they are exact decimals. BigDecimal = arbitrary-precision decimal number; used for money and
        // rates because double (floating point) cannot represent values like 0.1 exactly.
        @NotNull @DecimalMin("0.0") @DecimalMax("0.5") BigDecimal annualProfitRate,
        @NotNull @DecimalMin("0.1") @DecimalMax("1.0") BigDecimal maxFinanceToValue) {
}
