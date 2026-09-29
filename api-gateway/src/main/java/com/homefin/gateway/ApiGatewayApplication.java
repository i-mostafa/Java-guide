package com.homefin.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single entry point for clients (web/mobile). Responsibilities:
 *  - routing to services discovered through Eureka (lb://service-name)
 *  - edge authentication (reject requests without a valid JWT early)
 *  - cross-cutting concerns: request logging, tracing, (rate limiting, CORS...)
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
