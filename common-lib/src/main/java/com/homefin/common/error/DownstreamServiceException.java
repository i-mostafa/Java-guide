package com.homefin.common.error;

import org.springframework.http.HttpStatus;

/** A dependency (another service / 3rd-party API) failed or is unavailable -> 503. */
public class DownstreamServiceException extends ApiException {

    public DownstreamServiceException(String dependency, String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "downstream-unavailable",
                "%s is unavailable: %s".formatted(dependency, message), cause);
    }
}
