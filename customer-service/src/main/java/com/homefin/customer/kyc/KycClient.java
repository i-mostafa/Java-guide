package com.homefin.customer.kyc;

import com.homefin.common.error.ApiException;
import com.homefin.common.error.BusinessRuleException;
import com.homefin.common.error.DownstreamServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Wraps the 3rd-party KYC (identity verification) provider.
 *
 * <p>Resilience (configured in application.yml under resilience4j.*):
 * <ul>
 *   <li>{@code @Retry}: retries transient failures (timeouts, 5xx) with exponential backoff</li>
 *   <li>{@code @CircuitBreaker}: after too many failures stop calling the provider for a while (fail fast)</li>
 *   <li>fallbackMethod: converts the final failure into a clean 503 for our API clients</li>
 * </ul>
 *
 * <p>These annotations work through Spring AOP PROXIES: they only apply when the method is called
 * from ANOTHER bean (CustomerService -> KycClient), never via this.verify(...) inside the class.
 * At startup Spring sees the annotations (runtime) and, instead of injecting this object directly, injects a
 * generated wrapper that runs "circuit breaker -> retry -> real method" around each call. Think of it as
 * automatically applying {@code pRetry(() => breaker.fire(() => verify(...)))} in Node.
 *
 * <p>Other annotations: {@code @Slf4j} (Lombok, compile time) generates the {@code log} field; {@code @Component}
 * (runtime) registers this class as a singleton bean; {@code @RequiredArgsConstructor} (Lombok, compile time)
 * generates the constructor that Spring uses to inject the {@code kycRestClient} bean from KycClientConfig.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KycClient {

    // Injected by type (RestClient). The field name matches the bean name "kycRestClient", which would also
    // disambiguate if several RestClient beans existed.
    private final RestClient kycRestClient;

    // @Retry(name = "kyc") (runtime, AOP): apply the "kyc" retry policy from application.yml. When all attempts fail
    // (or the call isn't retryable), Resilience4j calls the method named in fallbackMethod with the same arguments
    // plus the exception.
    // @CircuitBreaker(name = "kyc") (runtime, AOP): count successes/failures; when OPEN, throw immediately
    // (CallNotPermittedException) without calling the provider, which then also ends in the fallback.
    @Retry(name = "kyc", fallbackMethod = "fallback")
    @CircuitBreaker(name = "kyc")
    public KycVerificationResponse verify(KycVerificationRequest request, String idempotencyKey) {
        log.debug("Calling KYC provider");
        // Fluent request builder, similar to axios.post(url, body, { headers }). Synchronous: this thread blocks
        // until the response arrives or the read timeout hits (Java uses threads, not an event loop, here).
        return kycRestClient.post()
                .uri("/kyc/v1/verifications")
                // Same key on every retry -> the provider can de-duplicate our POST.
                .header("Idempotency-Key", idempotencyKey)
                .body(request)   // serialized to JSON by Jackson
                .retrieve()
                // onStatus(predicate, handler): HttpStatusCode::is4xxClientError is a method reference used as the
                // predicate (code -> code.is4xxClientError()). "(req, res) -> { ... }" is a two-argument lambda
                // with a block body. 4xx = our request is bad; retrying won't help, so throw a business error.
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new BusinessRuleException("kyc-request-rejected",
                            "KYC provider rejected the request (" + res.getStatusCode().value() + ")");
                })
                // 5xx = provider problem; KycProviderException is listed as retryable in application.yml.
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new KycProviderException("KYC provider error " + res.getStatusCode().value());
                })
                // Parse the JSON body into this type. "X.class" passes the class object so the library knows the
                // target type at runtime (Java generics are erased at runtime, unlike a TS type argument).
                .body(KycVerificationResponse.class);
    }

    /** Signature = original params + Throwable. Called after retries are exhausted / circuit open. */
    // @SuppressWarnings("unused") (compile time): silence the IDE/compiler warning that this private method is never
    // called. It is called, but by Resilience4j via reflection (looked up by name), which tools can't see.
    // Throwable = the root of all errors and exceptions in Java (like "unknown" in a JS catch).
    @SuppressWarnings("unused")
    private KycVerificationResponse fallback(KycVerificationRequest request, String idempotencyKey, Throwable t) {
        // instanceof pattern matching: if t is an ApiException, also declare "apiException" already cast to that
        // type. Like "if (t instanceof ApiException) { const apiException = t; ... }" with TS narrowing.
        if (t instanceof ApiException apiException) {
            throw apiException; // our own business errors pass through unchanged
        }
        log.warn("KYC provider unavailable: {}", t.toString());
        // Becomes HTTP 503 via common-lib's GlobalExceptionHandler; the original exception is kept as the "cause".
        throw new DownstreamServiceException("KYC provider", t.getMessage(), t);
    }
}
