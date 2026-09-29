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

@Slf4j
@Component
@RequiredArgsConstructor
public class ValuationGateway {

    private final ValuationClient client;

    @Retry(name = "valuation", fallbackMethod = "fallback")
    @CircuitBreaker(name = "valuation")
    public ValuationResponse estimate(ValuationRequest request) {
        try {
            return client.estimate(request);
        } catch (HttpClientErrorException.NotFound e) {
            throw new BusinessRuleException("property-not-found",
                    "Property '%s' is unknown to the valuation provider".formatted(request.propertyReference()));
        }
    }

    @SuppressWarnings("unused")
    private ValuationResponse fallback(ValuationRequest request, Throwable t) {
        if (t instanceof ApiException api) {
            throw api;
        }
        log.warn("Valuation provider unavailable: {}", t.toString());
        throw new DownstreamServiceException("Valuation provider", t.getMessage(), t);
    }
}
