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
 */
@Configuration
public class FeignConfig {

    @Bean
    RequestInterceptor bearerTokenRelayInterceptor() {
        return template -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                template.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtAuth.getToken().getTokenValue());
            }
        };
    }
}
