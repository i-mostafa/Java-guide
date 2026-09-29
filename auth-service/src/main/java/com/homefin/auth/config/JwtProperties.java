package com.homefin.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Type-safe configuration bound from `app.jwt.*` in application.yml.
 * @Validated makes the app FAIL FAST at startup if config is missing/invalid
 * (much better than discovering `undefined` at runtime, as with process.env).
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotBlank String audience,
        @NotNull Duration accessTokenTtl) {
}
