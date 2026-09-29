package com.homefin.application.config;

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
 * <p>Role: builds the Spring Security filter chain, i.e. the ordered list of servlet filters every
 * HTTP request passes through before reaching a controller. TS analogy: the global auth middleware
 * stack in Express ({@code app.use(jwtMiddleware)}) or a global NestJS {@code AuthGuard}.
 * Public routes are whitelisted; everything else needs a valid Bearer token (otherwise 401).
 * Fine-grained role checks live on controller methods ({@code @PreAuthorize}).
 */
// @Configuration (runtime): Spring calls the @Bean methods below at startup.
@Configuration
// @EnableMethodSecurity (runtime): activates @PreAuthorize / @PostAuthorize on methods. Spring wraps those
// beans in a proxy that evaluates the expression before/after the real method runs.
@EnableMethodSecurity
public class SecurityConfig {

    // @Bean (runtime): Spring injects a pre-configured HttpSecurity builder as the parameter
    // (method parameters of @Bean methods are resolved from the container, like constructor injection).
    // "throws Exception": Java's CHECKED exceptions must be either caught or declared in the signature;
    // HttpSecurity.build() declares a checked Exception, so we pass it up to Spring.
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Fluent builder: each call configures one aspect and returns the builder (like a chained query builder).
        return http
                // Method reference `AbstractHttpConfigurer::disable` = shorthand for the lambda `c -> c.disable()`.
                // CSRF protection is for cookie sessions; a stateless Bearer-token API doesn't need it.
                .csrf(AbstractHttpConfigurer::disable)
                // STATELESS: never create an HTTP session; every request must carry its own JWT.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Rules are checked top to bottom, first match wins (like Express route order).
                // In the patterns, "**" matches any number of path segments.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                // Validate "Authorization: Bearer <jwt>" using the jwk-set-uri / issuer from application.yml.
                // Customizer.withDefaults() = "use the default settings" (a no-op customizer).
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }
}
