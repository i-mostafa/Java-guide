package com.homefin.common.error;

import org.springframework.http.HttpStatus;

/**
 * A dependency (another service / 3rd-party API) failed or is unavailable -> 503.
 *
 * <p>Wraps the original error as the "cause" so the stack trace is kept in logs while the client
 * only sees a generic message. TS analogy: {@code new Error(msg, { cause: err })}.
 */
public class DownstreamServiceException extends ApiException {

    // Throwable is the root of all errors/exceptions in Java (like "unknown" caught in a TS catch).
    public DownstreamServiceException(String dependency, String message, Throwable cause) {
        // String.formatted(...) is printf-style formatting: %s is replaced by each argument in order.
        // Equivalent to the TS template literal `${dependency} is unavailable: ${message}`.
        super(HttpStatus.SERVICE_UNAVAILABLE, "downstream-unavailable",
                "%s is unavailable: %s".formatted(dependency, message), cause);
    }
}
