package com.homefin.application.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Global OpenAPI (Swagger) metadata for this service's API docs.
 *
 * <p>Role: springdoc reads these annotations at runtime when generating the spec served at
 * /v3/api-docs and rendered by Swagger UI at /swagger-ui.html. It declares a "bearerAuth" JWT scheme
 * and applies it to all endpoints, so Swagger UI shows an "Authorize" button.
 * TS analogy: NestJS {@code new DocumentBuilder().setTitle(...).addBearerAuth().build()}.
 */
// @Configuration (runtime): makes Spring load this class, so springdoc can find the annotations on it.
@Configuration
// @OpenAPIDefinition (runtime, read by springdoc): title/version of the API and a default security
// requirement. Annotation attributes can themselves be annotations: info = @Info(...).
@OpenAPIDefinition(info = @Info(title = "Application Service API", version = "v1"),
        security = @SecurityRequirement(name = "bearerAuth"))
// @SecurityScheme (runtime, read by springdoc): defines what "bearerAuth" means (HTTP Authorization: Bearer JWT).
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
