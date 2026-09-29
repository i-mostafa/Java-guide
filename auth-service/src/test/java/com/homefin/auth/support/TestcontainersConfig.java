package com.homefin.auth.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Real Postgres + Kafka in Docker for integration tests.
 * {@code @ServiceConnection} wires spring.datasource.* and spring.kafka.* to the containers automatically.
 *
 * <p>Node analogy: the "testcontainers" npm package started in jest's globalSetup, with the
 * resulting host/port written into process.env before the app boots. Here Spring starts the
 * containers as beans and overrides the connection settings for you.
 */
// @TestConfiguration (runtime, tests only): a configuration class that is NOT picked up by component
// scanning; tests opt in with @Import(TestcontainersConfig.class).
// proxyBeanMethods = false: skip Spring's subclass proxy for this class (faster; fine because the
// @Bean methods don't call each other).
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    // @Bean: the container is a Spring bean; Spring starts it before the app needs it and stops it
    // with the context. @ServiceConnection: derive spring.datasource.url/username/password from it.
    // "PostgreSQLContainer<?>": "?" is a wildcard type argument = "some type, don't care which"
    // (the class is generic over itself), roughly PostgreSQLContainer<unknown> in TS.
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:17-alpine");
    }

    // Same for Kafka: sets spring.kafka.bootstrap-servers to the container's mapped port.
    @Bean
    @ServiceConnection
    KafkaContainer kafka() {
        return new KafkaContainer("apache/kafka-native:3.8.0");
    }
}
