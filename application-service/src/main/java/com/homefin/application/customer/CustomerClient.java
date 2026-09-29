package com.homefin.application.customer;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Declarative HTTP client for ANOTHER microservice. Feign generates the implementation.
 * name = Eureka service id -> resolved and load-balanced across instances at call time.
 * url  = optional override (e.g. point at WireMock in tests); empty -> use discovery.
 *
 * <p>Role: you only write an interface; at startup Feign (enabled by {@code @EnableFeignClients} on the
 * main class) creates a runtime proxy class implementing it and registers it as a bean. Calling
 * {@code currentCustomer()} performs {@code GET http://customer-service/api/customers/me} and parses the JSON
 * into a CustomerSummary. TS analogy: a typed API client generated from an interface (like a tRPC /
 * openapi-typescript client, or an axios wrapper you never had to write by hand).
 * The caller's JWT is added by the interceptor in FeignConfig (token relay).
 */
// @FeignClient (runtime): "generate an HTTP client for this interface".
// "${app.clients.customer-service.url:}" is a property placeholder resolved from configuration at startup:
// the value after ':' is the default (here: empty string), like process.env.X ?? ''.
@FeignClient(name = "customer-service", url = "${app.clients.customer-service.url:}", path = "/api/customers")
// interface = a contract with method signatures only (like a TS interface), but it exists at runtime,
// so a library can generate an implementation for it.
public interface CustomerClient {

    // @GetMapping (runtime): the same annotation used on controllers; here Feign reads it to know the HTTP
    // method and path. Interface methods are implicitly public and abstract.
    @GetMapping("/me")
    CustomerSummary currentCustomer();
}
