package com.homefin.auth.auth;

import com.homefin.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        // Same message for "unknown email" and "wrong password" -> no user enumeration.
        super(HttpStatus.UNAUTHORIZED, "invalid-credentials", "Invalid email or password");
    }
}
