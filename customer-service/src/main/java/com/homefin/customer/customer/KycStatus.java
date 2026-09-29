// Namespace = folder path. The "customer" feature package groups entity, repository, service and controller
// together (package-by-feature), similar to a NestJS feature module folder.
package com.homefin.customer.customer;

/**
 * The lifecycle state of a customer's identity verification (KYC = "Know Your Customer").
 *
 * <p>An {@code enum} is a fixed set of named constants, like a TypeScript string enum or a union type
 * {@code 'PENDING' | 'VERIFIED' | 'REJECTED'}. Unlike TS, each constant is a real singleton object and enums can
 * have fields and methods. The entity stores it in the DB as its name (e.g. "PENDING") and Jackson serializes
 * it to JSON as that same string.
 */
public enum KycStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
