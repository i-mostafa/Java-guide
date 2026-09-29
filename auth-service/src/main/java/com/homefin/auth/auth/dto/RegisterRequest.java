package com.homefin.auth.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO with Bean Validation (Jakarta Validation) annotations.
 * Equivalent of a zod / class-validator schema in Node. Triggered by {@code @Valid} in the controller.
 *
 * <p>JSON body of POST /api/auth/register. Validation runs at runtime (Hibernate Validator) before
 * the controller method is entered; every failing field is reported in the 400 response.
 */
public record RegisterRequest(
        // Several annotations can stack on one record component.
        // @Size(max = 255): string length limit. @Schema (springdoc): example value shown in Swagger UI.
        @NotBlank @Email @Size(max = 255)
        @Schema(example = "jane.doe@example.com")
        String email,

        // "message" overrides the default error text returned to the client.
        // 72 = bcrypt's input limit: longer passwords would be silently truncated.
        @NotBlank
        @Size(min = 10, max = 72, message = "must be between 10 and 72 characters")
        // @Pattern: regex check. Backslashes must be doubled in Java strings ("\\d" = regex \d),
        // because Java has no regex literal like JS's /.../.
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
