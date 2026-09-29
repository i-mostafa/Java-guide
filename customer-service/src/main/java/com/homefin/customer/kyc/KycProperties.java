package com.homefin.customer.kyc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// java.time.Duration = a length of time (e.g. 2 seconds). Spring converts YAML values like "2s" or "500ms" into it.
import java.time.Duration;

/**
 * Typed, validated settings for the KYC provider, bound from the {@code app.kyc.*} block of application.yml.
 * The TS equivalent is a config module that parses {@code process.env} through a zod schema once at startup and
 * exports a typed object.
 *
 * <ul>
 *   <li>{@code @ConfigurationProperties(prefix = "app.kyc")} (runtime): at startup Spring copies
 *       {@code app.kyc.base-url} into {@code baseUrl}, {@code connect-timeout} into {@code connectTimeout}, etc.
 *       (kebab-case to camelCase). It's registered as a bean thanks to {@code @ConfigurationPropertiesScan} on the
 *       application class, so other beans can simply ask for a {@code KycProperties} in their constructor.</li>
 *   <li>{@code @Validated} (runtime): run the Bean Validation constraints below after binding. If a value is
 *       missing the app refuses to start (fail fast), instead of failing on the first KYC call.</li>
 *   <li>{@code @NotBlank}: string must be non-null and contain a non-whitespace character.
 *       {@code @NotNull}: value must be present. Like {@code z.string().min(1)} / required fields.</li>
 * </ul>
 * A {@code record} fits well here: settings are immutable once bound.
 */
@Validated
@ConfigurationProperties(prefix = "app.kyc")
public record KycProperties(
        @NotBlank String baseUrl,
        @NotBlank String apiKey,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {
}
