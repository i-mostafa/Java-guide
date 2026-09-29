package com.homefin.common.error;

import org.springframework.http.HttpStatus;

/** A request that is syntactically valid but violates a business rule -> 422. */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
