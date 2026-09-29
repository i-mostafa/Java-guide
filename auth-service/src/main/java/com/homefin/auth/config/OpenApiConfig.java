package com.homefin.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) metadata for this service. springdoc generates the spec at /v3/api-docs and
 * the UI at /swagger-ui.html by scanning the controllers at runtime.
 *
 * <p>NestJS analogy: {@code new DocumentBuilder().setTitle(...).addBearerAuth()} in main.ts.
 * The empty class only exists to carry the annotations.
 */
@Configuration
// @OpenAPIDefinition: the document's title/version. Note the nested annotation @Info(...) as a value.
@OpenAPIDefinition(info = @Info(title = "Auth Service API", version = "v1"))
// @SecurityScheme: declares the "bearerAuth" scheme that AuthController's @SecurityRequirement refers
// to; adds the "Authorize" button for pasting a JWT in Swagger UI.
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
