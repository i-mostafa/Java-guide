package com.homefin.customer.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata for the generated OpenAPI (Swagger) document, served by springdoc at /v3/api-docs and /swagger-ui.html.
 * The equivalent of NestJS's {@code new DocumentBuilder().setTitle(...).addBearerAuth()}.
 *
 * <p>All annotations here are read at runtime by springdoc when it builds the spec:
 * <ul>
 *   <li>{@code @Configuration}: makes this class a Spring-managed config class so springdoc finds it.</li>
 *   <li>{@code @OpenAPIDefinition}: title/version, and a global security requirement meaning "every endpoint
 *       needs bearerAuth" (Swagger UI shows the lock icon and an Authorize button).</li>
 *   <li>{@code @SecurityScheme}: defines what "bearerAuth" is: an HTTP Authorization: Bearer JWT header.</li>
 * </ul>
 * Annotations can be nested as values of other annotations ({@code info = @Info(...)}), and their attributes are
 * named arguments ({@code title = "..."}), a bit like an options object literal.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Customer Service API", version = "v1"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
