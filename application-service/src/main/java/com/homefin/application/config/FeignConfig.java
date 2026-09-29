package com.homefin.application.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Token relay: forward the caller's JWT on service-to-service calls so the downstream service
 * knows WHO the end user is and can apply its own authorization rules.
 * (For calls without a user - batch jobs - use the OAuth2 client-credentials flow instead.)
 *
 * <p>Role: registers a Feign {@code RequestInterceptor}, which Feign runs before EVERY outgoing
 * request of every Feign client (here: CustomerClient). TS analogy: an axios request interceptor,
 * {@code axios.interceptors.request.use(cfg => { cfg.headers.Authorization = 'Bearer ' + token; return cfg; })},
 * where the token is taken from the current incoming request.
 *
 * <p>How it finds "the current request's user": Spring Security stores the authenticated user in
 * {@code SecurityContextHolder}, which is thread-local. Spring MVC handles each HTTP request on its own
 * thread, so this is similar to reading from an AsyncLocalStorage store set by an auth middleware.
 */
// @Configuration (runtime): marks a class whose @Bean methods Spring calls at startup to create beans.
// Like a NestJS module's "providers: [{ provide: X, useFactory: () => ... }]".
@Configuration
public class FeignConfig {

    // @Bean (runtime): Spring calls this method ONCE at startup and keeps the returned object as a
    // singleton bean. Other beans that need a RequestInterceptor get this one injected; Feign picks up
    // all RequestInterceptor beans automatically.
    // No access modifier = "package-private" (visible only inside this package). Spring can still call it.
    @Bean
    RequestInterceptor bearerTokenRelayInterceptor() {
        // Lambda: `template -> { ... }` is `(template) => { ... }`. RequestInterceptor is a
        // "functional interface" (exactly one abstract method), so a lambda can implement it directly.
        // This code runs on every outgoing Feign call, not at startup.
        return template -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // Pattern-matching instanceof: checks the type AND declares a typed variable `jwtAuth` in one step.
            // TS equivalent: `if (auth instanceof JwtAuthenticationToken) { const jwtAuth = auth; ... }`.
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                template.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtAuth.getToken().getTokenValue());
            }
        };
    }
}
