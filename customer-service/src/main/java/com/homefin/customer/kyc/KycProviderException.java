package com.homefin.customer.kyc;

/**
 * Transient provider failure (5xx) - worth retrying.
 *
 * <p>A custom error type, like {@code class KycProviderException extends Error} in TS. It is listed under
 * {@code resilience4j.retry.instances.kyc.retry-exceptions} in application.yml, so the retry policy re-attempts
 * calls that throw it, and it counts as a failure for the circuit breaker.
 *
 * <p>Java has two kinds of exceptions: "checked" ones (subclasses of {@code Exception}) must be declared with
 * {@code throws} and handled by every caller, and "unchecked" ones (subclasses of {@code RuntimeException}) don't.
 * Extending {@code RuntimeException} makes this unchecked, so it behaves like a normal JS throw. Spring code
 * almost always uses unchecked exceptions.
 */
public class KycProviderException extends RuntimeException {

    // A constructor: same name as the class, no return type. Called by "new KycProviderException("...")".
    public KycProviderException(String message) {
        // super(...) calls the parent class's constructor, exactly like super(message) in a TS subclass of Error.
        super(message);
    }
}
