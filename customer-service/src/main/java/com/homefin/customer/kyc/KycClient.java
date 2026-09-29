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
 * Resilience (configured in application.yml under resilience4j.*):
 *  - @Retry: retries transient failures (timeouts, 5xx) with exponential backoff
 *  - @CircuitBreaker: after too many failures stop calling the provider for a while (fail fast)
 *  - fallbackMethod: converts the final failure into a clean 503 for our API clients
 *
 * These annotations work through Spring AOP PROXIES: they only apply when the method is called
 * from ANOTHER bean (CustomerService -> KycClient), never via this.verify(...) inside the class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KycClient {

    private final RestClient kycRestClient;

    @Retry(name = "kyc", fallbackMethod = "fallback")
    @CircuitBreaker(name = "kyc")
    public KycVerificationResponse verify(KycVerificationRequest request, String idempotencyKey) {
        log.debug("Calling KYC provider");
        return kycRestClient.post()
                .uri("/kyc/v1/verifications")
                // Same key on every retry -> the provider can de-duplicate our POST.
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new BusinessRuleException("kyc-request-rejected",
                            "KYC provider rejected the request (" + res.getStatusCode().value() + ")");
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new KycProviderException("KYC provider error " + res.getStatusCode().value());
                })
                .body(KycVerificationResponse.class);
    }

    /** Signature = original params + Throwable. Called after retries are exhausted / circuit open. */
    @SuppressWarnings("unused")
    private KycVerificationResponse fallback(KycVerificationRequest request, String idempotencyKey, Throwable t) {
        if (t instanceof ApiException apiException) {
            throw apiException; // our own business errors pass through unchanged
        }
        log.warn("KYC provider unavailable: {}", t.toString());
        throw new DownstreamServiceException("KYC provider", t.getMessage(), t);
    }
}
