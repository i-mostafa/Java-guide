package com.homefin.auth.auth.dto;

// Jakarta Bean Validation constraint annotations (the "zod rules" of Java).
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * JSON body of POST /api/auth/login: {@code {"email": "...", "password": "..."}}.
 *
 * <p>A record (immutable DTO) whose components carry validation annotations. TS analogy:
 * {@code z.object({ email: z.string().email(), password: z.string().min(1) })}. The rules only run
 * when the controller parameter is marked {@code @Valid}; Jackson creates the record from JSON.
 */
// @NotBlank (runtime, Bean Validation): not null and contains a non-whitespace character.
// @Email (runtime): must look like an email address.
public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {

    // Records generate toString() with ALL fields; we override it so the password never ends up in logs.
    @Override
    public String toString() {
        return "LoginRequest[email=%s]".formatted(email);
    }
}
