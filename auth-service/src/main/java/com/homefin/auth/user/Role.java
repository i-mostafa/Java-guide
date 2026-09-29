package com.homefin.auth.user;

/**
 * The roles a user can have. Stored as text in the users.role column and put into the JWT
 * "roles" claim.
 *
 * <p>A Java {@code enum} is a class with a fixed set of instances, like a TS string enum
 * {@code enum Role { CUSTOMER = 'CUSTOMER', ADMIN = 'ADMIN' }}. {@code Role.CUSTOMER.name()} gives
 * "CUSTOMER" and {@code Role.valueOf("ADMIN")} parses one. Enums can also have fields and methods.
 */
public enum Role {
    CUSTOMER,
    ADMIN
}
