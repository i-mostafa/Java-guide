package com.homefin.application.valuation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Typed settings for the valuation provider, bound from the {@code app.valuation.*} keys in application.yml
 * (base-url, api-key, connect-timeout, read-timeout).
 *
 * <p>Role: bound and validated by Spring at startup (found via {@code @ConfigurationPropertiesScan}); injected
 * into ValuationClientConfig. If a value is missing the app fails to start instead of failing on the first
 * request. TS analogy: {@code z.object({ baseUrl: z.string().min(1), ... }).parse(process.env)} at boot.
 */
// @Validated (runtime): run the constraints below during binding.
@Validated
// @ConfigurationProperties (runtime): bind keys under "app.valuation"; kebab-case YAML keys -> camelCase components.
@ConfigurationProperties(prefix = "app.valuation")
public record ValuationProperties(
        // @NotBlank: not null and not empty/whitespace.
        @NotBlank String baseUrl,
        @NotBlank String apiKey,
        // java.time.Duration = a length of time. Spring converts strings like "2s", "300ms", "1m" automatically.
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {
}
