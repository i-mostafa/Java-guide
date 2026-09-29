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

/**
 * JSON request body of {@code POST /api/applications}, with its validation rules attached.
 *
 * <p>Role: Jackson deserializes the body into this record, then (because the controller parameter is marked
 * {@code @Valid}) Bean Validation checks every constraint annotation at runtime. All violations are collected
 * and returned together as a 400 "validation-failed" problem response by common-lib's exception handler.
 *
 * <p>TS analogy: a zod schema, e.g. {@code z.object({ propertyReference: z.string().max(50).regex(...), ... })},
 * with the class-level {@code @ValidFinanceRatio} playing the role of {@code .refine()} across fields.
 * Or class-validator decorators on a NestJS DTO with the ValidationPipe.
 */
// @ValidFinanceRatio (runtime, our custom constraint): cross-field check financeAmount / propertyValue <= 0.80.
@ValidFinanceRatio(maxRatio = 0.80)
public record CreateApplicationRequest(
        // @NotBlank: not null and not only whitespace. @Size(max): string length limit.
        // @Pattern: must match the regex (Java regexes are strings, so no /.../ delimiters).
        // @Schema (springdoc, runtime): example value shown in Swagger UI; no effect on validation.
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Z0-9-]+$", message = "use capitals, digits and '-'")
        @Schema(example = "DXB-MARINA-1204")
        String propertyReference,

        @NotBlank @Size(max = 100) @Schema(example = "Dubai")
        String city,

        // @NotNull: required. Enum values are checked by Jackson (unknown name -> 400).
        @NotNull
        PropertyType propertyType,

        // @DecimalMin("..."): minimum (inclusive), given as a string so it is an exact decimal.
        // @Digits(integer = 12, fraction = 2): at most 12 digits before and 2 after the decimal point,
        // matching the numeric(14, 2) DB column.
        @NotNull @DecimalMin("100000.00") @Digits(integer = 12, fraction = 2)
        @Schema(example = "1500000.00")
        BigDecimal propertyValue,

        @NotNull @DecimalMin("50000.00") @Digits(integer = 12, fraction = 2)
        @Schema(example = "1000000.00")
        BigDecimal financeAmount,

        // Integer (the object "wrapper" of int) instead of int, so a missing field arrives as null and
        // @NotNull can report it. With primitive int a missing field would silently become 0.
        // @Min/@Max: inclusive integer bounds (1 to 25 years).
        @NotNull @Min(12) @Max(300)
        @Schema(example = "300")
        Integer tenureMonths) {
}
