package com.homefin.common.error;

import org.springframework.http.HttpStatus;

/**
 * The request conflicts with the current state of a resource -> 409
 * (e.g. "email already registered").
 *
 * <p>Like NestJS's {@code ConflictException}: throw it anywhere and the global handler
 * renders the HTTP response.
 */
public class ConflictException extends ApiException {

    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
