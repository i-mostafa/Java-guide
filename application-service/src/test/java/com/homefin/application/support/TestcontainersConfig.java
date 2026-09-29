package com.homefin.application.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Real Postgres + Kafka in Docker for integration tests.
 * {@code @ServiceConnection} wires spring.datasource.* / spring.kafka.* to the containers automatically.
 *
 * <p>Role: imported by FinanceApplicationFlowIT via {@code @Import}. When the test context starts, Spring
 * creates these container beans, Testcontainers starts the Docker containers (random free ports), and Spring
 * Boot points the datasource and Kafka settings at them. Containers are stopped when the context closes.
 * TS analogy: {@code new PostgreSqlContainer().start()} in jest's globalSetup, then passing the connection URL
 * into your app config.
 */
// @TestConfiguration (runtime): a @Configuration meant only for tests; not picked up by normal component scanning.
// proxyBeanMethods = false: skip generating a CGLIB proxy subclass of this class (faster startup); fine because
// the @Bean methods never call each other.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    // @Bean (runtime): the container object becomes a bean. @ServiceConnection (runtime, Spring Boot 3.1+):
    // derive connection details (JDBC URL, user, password) from this container and use them instead of
    // spring.datasource.* from application.yml.
    // PostgreSQLContainer<?>: the class is generic (self-typed), "?" = any type argument.
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        // "<>" (the diamond) lets the compiler infer the generic type argument, like omitting it in TS.
        return new PostgreSQLContainer<>("postgres:17-alpine");
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafka() {
        return new KafkaContainer("apache/kafka-native:3.8.0");
    }
}
