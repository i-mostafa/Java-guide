package com.homefin.application.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/** Business parameters as configuration -> can differ per environment / be changed without a rebuild. */
@Validated
@ConfigurationProperties(prefix = "app.finance")
public record FinanceProperties(
        @NotNull @DecimalMin("0.0") @DecimalMax("0.5") BigDecimal annualProfitRate,
        @NotNull @DecimalMin("0.1") @DecimalMax("1.0") BigDecimal maxFinanceToValue) {
}
