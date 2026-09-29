package com.homefin.common.error;

import org.springframework.http.HttpStatus;

/**
 * The requested resource does not exist -> 404.
 *
 * <p>Typical use: {@code repository.findById(id).orElseThrow(() -> new NotFoundException("User", id))}.
 * Like NestJS's {@code NotFoundException}.
 */
public class NotFoundException extends ApiException {

    // Object = the root type of every Java class (roughly TS "unknown"/"any" for objects),
    // so any id type (UUID, Long, String) can be passed.
    public NotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, "resource-not-found", "%s '%s' was not found".formatted(resource, id));
    }
}
