package com.homefin.application.customer;

import com.homefin.common.error.ApiException;
import com.homefin.common.error.BusinessRuleException;
import com.homefin.common.error.DownstreamServiceException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Adds error translation + circuit breaking around the Feign client. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerGateway {

    private final CustomerClient client;

    @CircuitBreaker(name = "customer-service", fallbackMethod = "fallback")
    public CustomerSummary currentCustomer() {
        try {
            return client.currentCustomer();
        } catch (FeignException.NotFound e) {
            throw new BusinessRuleException("customer-profile-missing",
                    "No customer profile found. Complete registration first.");
        }
    }

    @SuppressWarnings("unused")
    private CustomerSummary fallback(Throwable t) {
        if (t instanceof ApiException api) {
            throw api;
        }
        log.warn("customer-service call failed: {}", t.toString());
        throw new DownstreamServiceException("customer-service", t.getMessage(), t);
    }
}
