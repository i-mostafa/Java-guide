package com.homefin.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point. {@code @SpringBootApplication} = {@code @Configuration} + {@code @EnableAutoConfiguration}
 * + {@code @ComponentScan} (scans this package and sub-packages for {@code @Component}/{@code @Service}/
 * {@code @Repository}/{@code @RestController}...).
 *
 * <p>auth-service owns user accounts: registration, login, and issuing signed JWTs that the
 * gateway and the other services verify. NestJS analogy: {@code main.ts} + the root AppModule,
 * except that modules/providers are discovered automatically by package scanning instead of
 * being listed by hand.
 */
// @SpringBootApplication (runtime): configuration class + auto-configuration + component scanning.
@SpringBootApplication
// @ConfigurationPropertiesScan (runtime): finds @ConfigurationProperties classes (JwtProperties,
// BootstrapAdminProperties) and registers them as beans filled from application.yml.
@ConfigurationPropertiesScan
public class AuthServiceApplication {

    // JVM entry point ("public static void main" is fixed by the language). args = CLI arguments.
    public static void main(String[] args) {
        // Starts the DI container, applies auto-configuration, runs Flyway, starts Tomcat on :8081.
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
