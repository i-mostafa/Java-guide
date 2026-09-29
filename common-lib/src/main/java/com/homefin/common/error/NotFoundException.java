package com.homefin.common.error;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, "resource-not-found", "%s '%s' was not found".formatted(resource, id));
    }
}
