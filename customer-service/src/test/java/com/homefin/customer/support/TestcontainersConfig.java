package com.homefin.customer.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Real Postgres + Kafka in Docker for integration tests.
 * {@code @ServiceConnection} wires spring.datasource.* / spring.kafka.* to the containers automatically.
 *
 * <p>In Node you'd start containers in a jest globalSetup with the testcontainers npm package and put their URLs in
 * {@code process.env}. Here the containers are Spring beans: when a test context that imports this class starts,
 * Spring starts the containers (Docker required), and {@code @ServiceConnection} (runtime) overrides the datasource
 * URL/credentials and Kafka bootstrap servers from application.yml with the containers' random host ports.
 * Flyway then migrates the fresh database as usual.
 *
 * <p>{@code @TestConfiguration} (runtime): a {@code @Configuration} meant only for tests. It is NOT picked up by
 * component scanning; a test must opt in with {@code @Import(TestcontainersConfig.class)}.
 * {@code proxyBeanMethods = false}: Spring skips generating a proxy subclass for this config class (faster startup;
 * fine because no bean method calls another).
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    // @Bean (runtime): Spring calls this once per test context and manages the container's lifecycle (start/stop).
    // PostgreSQLContainer<?>: the class is generic over itself (a "self type" used by its fluent API); "?" means we
    // don't care about the exact type argument here.
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        // "<>" (diamond): the compiler infers the type argument. The string is the Docker image to run.
        return new PostgreSQLContainer<>("postgres:17-alpine");
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafka() {
        return new KafkaContainer("apache/kafka-native:3.8.0");
    }
}
