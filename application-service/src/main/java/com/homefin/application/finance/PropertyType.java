package com.homefin.application.finance;

/**
 * Kind of property being financed. Accepted in JSON requests and stored in the DB by name.
 *
 * <p>TS analogy: {@code type PropertyType = 'APARTMENT' | 'VILLA' | 'TOWNHOUSE'}. Jackson rejects any
 * other string in a request body with a 400, so no extra validation is needed.
 */
// enum = a fixed set of named constants; each one is a singleton object of type PropertyType.
public enum PropertyType {
    APARTMENT,
    VILLA,
    TOWNHOUSE
}
