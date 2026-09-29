package com.homefin.common.web;

import com.homefin.common.error.GlobalExceptionHandler;
import com.homefin.common.security.JwtRolesConverterFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * A custom Spring Boot auto-configuration. Any service that has common-lib on its classpath
 * gets these beans automatically (registered in
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
 * This is exactly how every "spring-boot-starter-*" works under the hood.
 *
 * <p>Background: Spring keeps an "application context" - a DI container holding "beans"
 * (singleton objects it creates and wires together), like NestJS's module/provider system.
 * At startup Spring Boot reads that .imports file from every JAR, evaluates the conditions below
 * and, if they pass, registers the beans. TS analogy: a NestJS dynamic module that a library
 * auto-registers as global, so apps don't have to import it by hand.
 */
// @AutoConfiguration (Spring Boot, runtime): a @Configuration class loaded via the .imports file
// rather than via component scanning, and processed after the app's own configuration.
@AutoConfiguration
// @ConditionalOnWebApplication: only apply in servlet (Spring MVC) apps - NOT in the reactive
// api-gateway, which uses WebFlux and would not have the MVC classes.
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
// @Import: also register GlobalExceptionHandler as a bean (so its @RestControllerAdvice is active),
// even though it lives in a package the service's component scan wouldn't reach.
@Import(GlobalExceptionHandler.class)
public class CommonWebAutoConfiguration {

    // @Bean (Spring, runtime): Spring calls this method once at startup and stores the returned
    // object in the container; anything needing a JwtAuthenticationConverter gets this instance
    // injected. Like a NestJS provider with useFactory.
    // @ConditionalOnMissingBean: skip it if the service already defined its own converter bean,
    // so services can override the default.
    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        return JwtRolesConverterFactory.create();
    }
}
