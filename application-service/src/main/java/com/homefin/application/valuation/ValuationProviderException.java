package com.homefin.application.valuation;

/**
 * Thrown when the valuation provider answers with a 5xx (see ValuationClientConfig).
 *
 * <p>Role: a dedicated type so Resilience4j can be told "retry THIS" (retry-exceptions in application.yml)
 * without retrying everything. TS analogy: {@code class ValuationProviderError extends Error {}} checked with
 * {@code instanceof} in a retry predicate.
 *
 * <p>Extending RuntimeException makes it an UNCHECKED exception: callers aren't forced to catch it or declare
 * {@code throws}. (Subclasses of plain Exception are "checked" and must be handled or declared.)
 */
public class ValuationProviderException extends RuntimeException {

    // Constructor: same name as the class, no return type (like `constructor(message: string)` in TS).
    // super(message) calls the parent class constructor, as in TS.
    public ValuationProviderException(String message) {
        super(message);
    }
}
