package com.homefin.application.customer;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Declarative HTTP client for ANOTHER microservice. Feign generates the implementation.
 * name = Eureka service id -> resolved and load-balanced across instances at call time.
 * url  = optional override (e.g. point at WireMock in tests); empty -> use discovery.
 */
@FeignClient(name = "customer-service", url = "${app.clients.customer-service.url:}", path = "/api/customers")
public interface CustomerClient {

    @GetMapping("/me")
    CustomerSummary currentCustomer();
}
