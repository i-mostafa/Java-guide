package com.homefin.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security = a chain of servlet filters (like a stack of Express middlewares)
 * configured through this SecurityFilterChain bean.
 *
 * <p>Defines: which URLs are public, that everything else needs a valid JWT, the password hashing
 * algorithm, and the AuthenticationManager used by the login endpoint. Express analogy:
 * {@code app.use(publicRoutes); app.use(passport.authenticate('jwt', { session: false }));}.
 */
@Configuration
// @EnableMethodSecurity (runtime): allows annotations like @PreAuthorize("hasRole('ADMIN')") on bean
// methods; Spring wraps those beans in proxies that check the rule before each call.
@EnableMethodSecurity // enables @PreAuthorize on methods
public class SecurityConfig {

    // "throws Exception": HttpSecurity's builder methods declare a checked Exception, so we must
    // either catch it or pass it on. Passing it on is fine - Spring would just fail at startup.
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Method reference: AbstractHttpConfigurer::disable == (csrf) -> csrf.disable().
                .csrf(AbstractHttpConfigurer::disable) // stateless API with bearer tokens -> no CSRF risk
                // Never create an HTTP session (no JSESSIONID cookie): every request carries its JWT.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Authorization rules, evaluated top to bottom (first match wins).
                // Paths ending in ** match any sub-path (e.g. everything under /swagger-ui).
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/.well-known/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                // Accept "Authorization: Bearer <jwt>", verified by the JwtDecoder bean from JwtKeyConfig.
                // Customizer.withDefaults() = "use the default settings" (a no-op configurer lambda).
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }

    /** Delegating encoder -> stores "{bcrypt}$2a$10$..." so the algorithm can be upgraded later. */
    // PasswordEncoder is an interface; this bean is its implementation injected into AuthService.
    // Node analogy: bcrypt.hash / bcrypt.compare behind one object.
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Built from our UserDetailsService + PasswordEncoder beans (DaoAuthenticationProvider). */
    // Spring Boot builds the AuthenticationManager internally; this bean method only exposes it so
    // that AuthService can have it injected.
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
