package com.homefin.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Service registry. Services register as e.g. "CUSTOMER-SERVICE" and call each other
 * by name (lb://customer-service) instead of hard-coded host:port.
 * On Kubernetes you would usually drop Eureka and use K8s Services/DNS instead.
 *
 * <p>Each service's Eureka client sends a heartbeat here every ~30s; the gateway and other
 * clients download the registry and load-balance across the listed instances.
 * Node analogy: a Consul agent (or a Redis hash of "service name -> instances") that every
 * service writes to and reads from. The dashboard is at http://localhost:8761.
 */
// @SpringBootApplication: config class + auto-configuration + component scanning (see config-server).
@SpringBootApplication
// @EnableEurekaServer (Spring Cloud Netflix, runtime): starts the Eureka registry server and its UI.
@EnableEurekaServer
public class DiscoveryServerApplication {

    // JVM entry point (like the "main" script in package.json). "static" = no instance needed.
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
