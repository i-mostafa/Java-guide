package com.homefin.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// java.time.Duration = a length of time; Spring converts YAML values like "15m" or "PT15M" into it.
import java.time.Duration;

/**
 * Type-safe configuration bound from `app.jwt.*` in application.yml.
 * {@code @Validated} makes the app FAIL FAST at startup if config is missing/invalid
 * (much better than discovering `undefined` at runtime, as with process.env).
 *
 * <p>TS analogy: parsing process.env with a zod schema once at boot and exporting the typed result.
 * YAML key {@code access-token-ttl} binds to the record component {@code accessTokenTtl}.
 */
// @Validated (Spring, runtime): run the Bean Validation annotations below when binding the config.
@Validated
// @ConfigurationProperties: bind all "app.jwt.*" properties into this record at startup.
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotBlank String audience,
        // @NotNull: the value must be present (it may be anything else).
        @NotNull Duration accessTokenTtl) {
}
