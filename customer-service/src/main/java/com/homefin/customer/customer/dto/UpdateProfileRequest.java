package com.homefin.customer.customer.dto;

import com.homefin.customer.customer.validation.Adult;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * JSON request body for updating the caller's profile ({@code PUT /api/customers/me}), validated before the
 * controller method runs. This is the Java counterpart of a zod schema / a NestJS DTO class with class-validator
 * decorators: Jackson parses the JSON into this immutable {@code record}, then, because the controller parameter
 * is marked {@code @Valid}, Bean Validation checks every annotation below at runtime. Any violation produces an
 * HTTP 400 with the field errors, and the controller method is never called.
 *
 * <p>Annotations used (all from Jakarta Bean Validation, checked at runtime):
 * <ul>
 *   <li>{@code @NotBlank}: not null, not empty, not only whitespace. {@code @NotNull}: not null.</li>
 *   <li>{@code @Size(max = 100)}: string length limit (matches the varchar(100) column).</li>
 *   <li>{@code @Pattern(regexp = ...)}: must match the regex; {@code message} overrides the error text.</li>
 *   <li>{@code @Past}: the date must be before today. {@code @Adult}: our custom constraint (at least 18 years).</li>
 * </ul>
 * {@code @Schema(example = ...)} (runtime, read by springdoc) only affects the OpenAPI docs / Swagger UI example.
 */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,

        // In Java string literals the backslash must itself be escaped, so "\\+" in source is the regex \+.
        // E.164 = "+" then country code and number, 8 to 15 digits in total, no leading zero.
        @NotBlank
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "must be in E.164 format, e.g. +971501234567")
        @Schema(example = "+971501234567")
        String phoneNumber,

        // Several constraints can stack on one field; all are checked and all violations are reported.
        @NotNull @Past @Adult
        @Schema(example = "1990-05-17")
        LocalDate dateOfBirth,

        @NotBlank
        @Pattern(regexp = "^[0-9A-Z-]{6,20}$", message = "must be 6-20 characters: digits, capitals or '-'")
        @Schema(example = "784-1990-1234567-1")
        String nationalId) {

    // @Override (compile time): replaces the record's generated toString. The generated one would print every
    // field, leaking phone number, birth date and national id (PII) into logs.
    @Override
    public String toString() {
        return "UpdateProfileRequest[firstName=%s, lastName=%s]".formatted(firstName, lastName);
    }
}
