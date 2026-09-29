package com.homefin.auth.auth;

import com.homefin.common.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown by {@link AuthService#login} when the email/password pair is wrong -> 401.
 *
 * <p>Extends the shared {@code ApiException} from common-lib, so the global exception handler turns
 * it into a Problem Details JSON response automatically. Like a custom
 * {@code class InvalidCredentialsError extends ApiError} in a Node app.
 */
public class InvalidCredentialsException extends ApiException {

    // A no-argument constructor, which also allows the constructor reference InvalidCredentialsException::new.
    public InvalidCredentialsException() {
        // Same message for "unknown email" and "wrong password" -> no user enumeration.
        super(HttpStatus.UNAUTHORIZED, "invalid-credentials", "Invalid email or password");
    }
}
