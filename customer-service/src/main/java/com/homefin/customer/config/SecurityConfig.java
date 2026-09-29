package com.homefin.customer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Resource server: validates the JWT on every request using auth-service's public keys (JWKS).
 * Roles come from the "roles" claim (converter provided by common-lib auto-configuration).
 *
 * <p>Spring Security is a chain of servlet filters that runs BEFORE any controller, like a stack of Express
 * middlewares ({@code app.use(passport.authenticate('jwt'))}). This class configures that chain.
 *
 * <ul>
 *   <li>{@code @Configuration} (runtime): Spring reads this class at startup and calls its {@code @Bean} methods.</li>
 *   <li>{@code @EnableMethodSecurity} (runtime): turns on {@code @PreAuthorize("hasRole('ADMIN')")} checks on
 *       individual methods (see CustomerController). Implemented with AOP proxies around those beans.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // @Bean (runtime): Spring calls this factory method once at startup and registers the returned object in the
    // DI container, like a NestJS { provide: X, useFactory: ... } provider. The HttpSecurity parameter is itself
    // injected by Spring. No "public" modifier = package-private (visible within this package only); Spring
    // doesn't need public access. "throws Exception" declares a checked exception the builder API may throw.
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // AbstractHttpConfigurer::disable is a METHOD REFERENCE: shorthand for the lambda c -> c.disable().
                // CSRF protection is for cookie-based sessions; a stateless bearer-token API doesn't need it.
                .csrf(AbstractHttpConfigurer::disable)
                // "s -> s.xxx()" is a LAMBDA (arrow function). Here: never create an HTTP session (no JSESSIONID
                // cookie); every request is authenticated from its own token.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Route rules, checked top to bottom; first match wins. "**" = any number of path segments.
                .authorizeHttpRequests(auth -> auth
                        // Health/metrics endpoints are public so Kubernetes/Prometheus can call them without a token.
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        // OpenAPI JSON and Swagger UI are public too.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Everything else requires a valid JWT (otherwise 401).
                        .anyRequest().authenticated())
                // Validate "Authorization: Bearer <jwt>" using the jwk-set-uri/issuer/audience from application.yml.
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }
}
