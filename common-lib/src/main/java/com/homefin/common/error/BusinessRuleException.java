package com.homefin.common.error;

import org.springframework.http.HttpStatus;

/**
 * A request that is syntactically valid but violates a business rule -> 422.
 *
 * <p>Example: "tenure must not exceed 25 years for this product". Thrown from service code;
 * {@code GlobalExceptionHandler} converts it to a 422 Problem Details response.
 * TS analogy: {@code class BusinessRuleError extends ApiError} with a fixed status.
 */
public class BusinessRuleException extends ApiException {

    // Public constructor: fixes the HTTP status and forwards to the protected parent constructor.
    // HttpStatus is an enum; UNPROCESSABLE_ENTITY is one of its constants (like HttpStatus.X in NestJS).
    public BusinessRuleException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
