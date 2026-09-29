package com.homefin.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Reactive security (WebFlux flavour: ServerHttpSecurity / SecurityWebFilterChain).
 * The gateway only checks "is the token valid?". Fine-grained authorization (roles, ownership)
 * is enforced again inside each service -> defence in depth / zero trust.
 *
 * <p>Spring Security works as a chain of filters (middlewares) in front of every request.
 * This class builds that chain with a fluent builder. Express analogy:
 * {@code app.use(publicPaths, next); app.use(requireValidJwt);}.
 */
// @Configuration (Spring, runtime): a class whose @Bean methods produce objects for the DI container.
// Found automatically by component scanning. Like a NestJS module's "providers" section.
@Configuration
// @EnableWebFluxSecurity: switches on Spring Security for a reactive (WebFlux) application.
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    // @Bean: Spring calls this once at startup; the returned filter chain is applied to every request.
    // No access modifier = "package-private" (visible only inside this package). That's enough,
    // because Spring calls it via reflection.
    // The parameter "http" is injected by Spring: a pre-configured builder bean.
    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // "ServerHttpSecurity.CsrfSpec::disable" is a method reference: shorthand for the
                // lambda csrf -> csrf.disable(). CSRF protection only matters for cookie sessions.
                .csrf(ServerHttpSecurity.CsrfSpec::disable) // stateless JWT API, no cookies
                // "ex -> ex.pathMatchers(...)...." is a lambda (arrow function) configuring the rules.
                // Rules are checked top to bottom; the first match wins.
                // Public: register, login and the JWKS keys under /.well-known (ant-style ** = any sub-path).
                .authorizeExchange(ex -> ex
                        .pathMatchers("/api/auth/register", "/api/auth/login", "/.well-known/**").permitAll()
                        .pathMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        // Everything else needs a valid JWT.
                        .anyExchange().authenticated())
                // Validate "Authorization: Bearer <jwt>" headers with defaults: keys come from the
                // jwk-set-uri, and issuer/audience checks from application.yml.
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }
}
