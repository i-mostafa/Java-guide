package com.homefin.application.finance.dto;

import com.homefin.application.finance.PropertyType;
import com.homefin.application.finance.validation.ValidFinanceRatio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@ValidFinanceRatio(maxRatio = 0.80)
public record CreateApplicationRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Z0-9-]+$", message = "use capitals, digits and '-'")
        @Schema(example = "DXB-MARINA-1204")
        String propertyReference,

        @NotBlank @Size(max = 100) @Schema(example = "Dubai")
        String city,

        @NotNull
        PropertyType propertyType,

        @NotNull @DecimalMin("100000.00") @Digits(integer = 12, fraction = 2)
        @Schema(example = "1500000.00")
        BigDecimal propertyValue,

        @NotNull @DecimalMin("50000.00") @Digits(integer = 12, fraction = 2)
        @Schema(example = "1000000.00")
        BigDecimal financeAmount,

        @NotNull @Min(12) @Max(300)
        @Schema(example = "300")
        Integer tenureMonths) {
}
