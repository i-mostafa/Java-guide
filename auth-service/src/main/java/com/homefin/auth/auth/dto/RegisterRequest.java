package com.homefin.auth.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO with Bean Validation (Jakarta Validation) annotations.
 * Equivalent of a zod / class-validator schema in Node. Triggered by @Valid in the controller.
 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 255)
        @Schema(example = "jane.doe@example.com")
        String email,

        @NotBlank
        @Size(min = 10, max = 72, message = "must be between 10 and 72 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "must contain upper and lower case letters, a digit and a symbol")
        @Schema(example = "S3cure#Passw0rd")
        String password,

        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName) {

    /** Never print passwords - records generate toString() including all fields by default! */
    @Override
    public String toString() {
        return "RegisterRequest[email=%s, firstName=%s, lastName=%s]".formatted(email, firstName, lastName);
    }
}
