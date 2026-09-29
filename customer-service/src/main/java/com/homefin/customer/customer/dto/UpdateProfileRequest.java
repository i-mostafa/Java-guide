package com.homefin.customer.customer.dto;

import com.homefin.customer.customer.validation.Adult;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,

        @NotBlank
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "must be in E.164 format, e.g. +971501234567")
        @Schema(example = "+971501234567")
        String phoneNumber,

        @NotNull @Past @Adult
        @Schema(example = "1990-05-17")
        LocalDate dateOfBirth,

        @NotBlank
        @Pattern(regexp = "^[0-9A-Z-]{6,20}$", message = "must be 6-20 characters: digits, capitals or '-'")
        @Schema(example = "784-1990-1234567-1")
        String nationalId) {

    @Override
    public String toString() {
        return "UpdateProfileRequest[firstName=%s, lastName=%s]".formatted(firstName, lastName);
    }
}
