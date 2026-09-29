package com.homefin.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Inject a Clock instead of calling Instant.now() directly -> time becomes testable.
 *
 * <p>Tests can pass {@code Clock.fixed(...)} instead - similar to {@code jest.useFakeTimers()},
 * but by plain dependency injection.
 */
// @Configuration (runtime): a class that contributes beans through @Bean methods.
@Configuration
public class ClockConfig {

    // @Bean: Spring calls this once and injects the returned Clock wherever one is requested
    // (e.g. TokenService's constructor). Package-private is enough for Spring.
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
