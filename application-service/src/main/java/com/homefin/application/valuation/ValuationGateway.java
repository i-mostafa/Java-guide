package com.homefin.application.valuation;

import com.homefin.common.error.ApiException;
import com.homefin.common.error.BusinessRuleException;
import com.homefin.common.error.DownstreamServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Resilient wrapper around the 3rd-party valuation API: retries, circuit breaker and error translation.
 *
 * <p>Role: FinanceApplicationService calls {@code estimate}; it gets either a valuation or one of our domain
 * exceptions (422 for unknown property, 503 when the provider is down). TS analogy: a service wrapping an
 * axios client with {@code p-retry} (exponential backoff) and an {@code opossum} circuit breaker.
 *
 * <p>How the annotations work: Spring hands other beans an AOP PROXY of this class. When
 * {@code estimate} is called through the proxy, the Resilience4j aspects wrap the real call: the circuit breaker
 * first rejects immediately if the circuit is open; otherwise the call runs, and failures listed in
 * {@code retry-exceptions} (application.yml) are retried with backoff. When retries are exhausted, the
 * {@code fallback} method is invoked. The aspect order (Retry outside CircuitBreaker by default) means each retry
 * attempt is counted by the breaker. A self-call from inside this class would bypass the proxy entirely.
 */
// @Slf4j, @RequiredArgsConstructor (Lombok, compile time): `log` field + constructor for `client`.
// @Component (runtime): a Spring bean.
@Slf4j
@Component
@RequiredArgsConstructor
public class ValuationGateway {

    private final ValuationClient client;

    // @Retry (runtime, Resilience4j via AOP): use the "valuation" retry config from application.yml; call the
    // method named "fallback" (found by reflection) once all attempts failed.
    // @CircuitBreaker (runtime, Resilience4j via AOP): use the "valuation" circuit breaker instance.
    @Retry(name = "valuation", fallbackMethod = "fallback")
    @CircuitBreaker(name = "valuation")
    public ValuationResponse estimate(ValuationRequest request) {
        try {
            return client.estimate(request);
        // HttpClientErrorException.NotFound = the exception RestClient throws for HTTP 404 (a nested class).
        } catch (HttpClientErrorException.NotFound e) {
            // A 404 is a business answer ("unknown property"), not an outage: not retried, and the circuit
            // breaker ignores BusinessRuleException (ignore-exceptions in application.yml).
            throw new BusinessRuleException("property-not-found",
                    "Property '%s' is unknown to the valuation provider".formatted(request.propertyReference()));
        }
    }

    // Fallback signature rule: same return type, same parameters as the protected method, plus a Throwable.
    @SuppressWarnings("unused")
    private ValuationResponse fallback(ValuationRequest request, Throwable t) {
        // Rethrow our own domain exceptions (e.g. the 422 above) unchanged.
        if (t instanceof ApiException api) {
            throw api;
        }
        log.warn("Valuation provider unavailable: {}", t.toString());
        throw new DownstreamServiceException("Valuation provider", t.getMessage(), t);
    }
}
