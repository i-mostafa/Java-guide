package com.homefin.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for exceptions that map cleanly to an HTTP status.
 * Unchecked (extends RuntimeException) - the idiomatic choice in Spring apps.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    protected ApiException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }
}
