package com.homefin.application.customer;

import com.homefin.common.error.ApiException;
import com.homefin.common.error.BusinessRuleException;
import com.homefin.common.error.DownstreamServiceException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adds error translation + circuit breaking around the Feign client.
 *
 * <p>Role: the rest of the app calls this gateway, never CustomerClient directly. It turns low-level HTTP
 * failures into our own domain exceptions (which common-lib's global handler maps to HTTP responses) and
 * stops hammering customer-service when it is down. TS analogy: a small service wrapping an axios client
 * with try/catch plus an "opossum" circuit breaker.
 *
 * <p>How the circuit breaker works: Spring does not hand other beans the raw CustomerGateway; it hands them
 * an AOP PROXY (a generated subclass) that intercepts calls to {@code @CircuitBreaker} methods, counts
 * failures, and when too many fail (see resilience4j settings in application.yml) short-circuits straight to
 * the fallback for a while. Because it is a proxy, it only applies to calls coming from OTHER beans; a method
 * of this class calling {@code this.currentCustomer()} would bypass it.
 */
// @Slf4j (Lombok, compile time): generates `private static final Logger log = LoggerFactory.getLogger(...)`.
@Slf4j
// @Component (runtime): register this class as a Spring bean (singleton) found by component scanning.
// Like marking a class @Injectable() and listing it as a provider in NestJS.
@Component
// @RequiredArgsConstructor (Lombok, compile time): generates a constructor taking every "final" field.
// Spring sees a single constructor and uses it for dependency injection (constructor injection),
// like NestJS `constructor(private readonly client: CustomerClient) {}`.
@RequiredArgsConstructor
public class CustomerGateway {

    // private = visible only inside this class. final = must be assigned once (in the constructor), then
    // never reassigned (like `readonly`).
    private final CustomerClient client;

    // @CircuitBreaker (runtime, Resilience4j via AOP proxy): name selects the config instance
    // "customer-service" in application.yml; fallbackMethod is looked up BY NAME (reflection) and called
    // when the call throws or the circuit is open. It must have the same return type and take the original
    // arguments plus a Throwable.
    @CircuitBreaker(name = "customer-service", fallbackMethod = "fallback")
    public CustomerSummary currentCustomer() {
        // try/catch works like in TS, but each catch clause matches by exception TYPE.
        try {
            return client.currentCustomer();
        // FeignException.NotFound is a nested static class of FeignException (thrown for HTTP 404).
        } catch (FeignException.NotFound e) {
            // "new" creates an object instance, as in TS. BusinessRuleException -> HTTP 422 via common-lib.
            // Unchecked exceptions (subclasses of RuntimeException) need no "throws" declaration.
            throw new BusinessRuleException("customer-profile-missing",
                    "No customer profile found. Complete registration first.");
        }
    }

    // @SuppressWarnings("unused") (compile time, javac/IDE): silences the "private method never used" warning,
    // because only Resilience4j calls this method, via reflection.
    @SuppressWarnings("unused")
    private CustomerSummary fallback(Throwable t) {
        // Our own ApiExceptions (e.g. the 422 above) are real answers, not outages: rethrow unchanged.
        if (t instanceof ApiException api) {
            throw api;
        }
        // {} placeholders are filled by the logger (SLF4J), like console.log('%s', x); cheaper than concatenation.
        log.warn("customer-service call failed: {}", t.toString());
        // DownstreamServiceException -> HTTP 503. Passing `t` as the cause keeps the original stack trace.
        throw new DownstreamServiceException("customer-service", t.getMessage(), t);
    }
}
