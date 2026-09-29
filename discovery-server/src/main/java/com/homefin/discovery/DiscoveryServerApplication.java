package com.homefin.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Service registry. Services register as e.g. "CUSTOMER-SERVICE" and call each other
 * by name (lb://customer-service) instead of hard-coded host:port.
 * On Kubernetes you would usually drop Eureka and use K8s Services/DNS instead.
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
