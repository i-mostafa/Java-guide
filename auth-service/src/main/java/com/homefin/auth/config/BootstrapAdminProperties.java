package com.homefin.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed settings bound from the {@code app.bootstrap-admin.*} keys in application.yml
 * (enabled, email, password), used by {@link AdminBootstrap}.
 *
 * <p>TS analogy: {@code const adminConfig = { enabled: env.ADMIN_ENABLED === 'true', email: ..., ... }},
 * but parsed and type-converted by Spring (kebab-case YAML keys map to camelCase names). Registered
 * as a bean thanks to {@code @ConfigurationPropertiesScan} on the application class.
 */
// @ConfigurationProperties (runtime): at startup Spring reads every property under the prefix and
// passes it to the record's constructor; the result is injectable like any other bean.
@ConfigurationProperties(prefix = "app.bootstrap-admin")
// "boolean" is the primitive true/false type.
public record BootstrapAdminProperties(boolean enabled, String email, String password) {
}
