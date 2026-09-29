package com.homefin.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single entry point for clients (web/mobile). Responsibilities:
 *  - routing to services discovered through Eureka (lb://service-name)
 *  - edge authentication (reject requests without a valid JWT early)
 *  - cross-cutting concerns: request logging, tracing, (rate limiting, CORS...)
 *
 * <p>The routes themselves are declared in application.yml, not in code. Node analogy: an Express
 * app using http-proxy-middleware plus a JWT-checking middleware in front of the microservices.
 * Unlike the other services this one runs on WebFlux/Netty (non-blocking, event-loop based).
 */
// @SpringBootApplication: marks the main config class; enables auto-configuration (here: the
// gateway, security, Eureka client) and scans this package and sub-packages for components
// such as RequestLoggingFilter and GatewaySecurityConfig.
@SpringBootApplication
public class ApiGatewayApplication {

    // JVM entry point. SpringApplication.run creates the DI container and starts the Netty server.
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
