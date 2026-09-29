package com.homefin.auth.auth;

import com.homefin.auth.auth.dto.LoginRequest;
import com.homefin.auth.auth.dto.RegisterRequest;
import com.homefin.auth.auth.dto.TokenResponse;
import com.homefin.auth.auth.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Express equivalent:  router.post('/api/auth/register', validate(schema), handler)
 *
 * <p>The HTTP layer for authentication: register, login and "who am I". It only translates
 * HTTP to method calls and back; the logic lives in {@link AuthService}. NestJS analogy: a
 * {@code @Controller('api/auth')} class. Spring MVC calls these methods on each matching HTTP
 * request, deserializes the JSON body into the parameter and serializes the return value to JSON.
 */
// @RestController (Spring, runtime): a @Component that handles HTTP requests; return values are
// written as JSON response bodies (no view templates).
@RestController
// @RequestMapping: common URL prefix for every handler in this class (like express.Router() mounted
// at "/api/auth").
@RequestMapping("/api/auth")
// @RequiredArgsConstructor (Lombok, compile time): generates a constructor taking every final field,
// i.e. AuthController(AuthService authService) { this.authService = authService; }.
// Spring sees that single constructor and injects the AuthService bean into it at startup -
// constructor injection, like NestJS's constructor(private readonly authService: AuthService).
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // @Operation (springdoc/OpenAPI): documentation for Swagger UI, like @ApiOperation in NestJS.
    // @PostMapping: handle POST /api/auth/register.
    // @ResponseStatus: reply 201 Created instead of the default 200.
    @Operation(summary = "Register a new customer account")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    // @RequestBody: bind the JSON body to this parameter (Jackson does the parsing).
    // @Valid: run the Bean Validation annotations on RegisterRequest first; on failure Spring throws
    // MethodArgumentNotValidException and GlobalExceptionHandler returns a 400.
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(authService.register(request));
    }

    @Operation(summary = "Exchange email/password for an access token")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // security = @SecurityRequirement(...): an annotation used as an annotation attribute value;
    // it marks the endpoint as needing the "bearerAuth" scheme (defined in OpenApiConfig) in Swagger UI.
    @Operation(summary = "Current user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    // @AuthenticationPrincipal: inject the authenticated principal - here the decoded, already
    // verified JWT (like req.user after a passport-jwt middleware). The route requires authentication
    // (see SecurityConfig), so the JWT is always present.
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        // "sub" holds the user id as text; UUID.fromString parses it back.
        return UserResponse.from(authService.getById(UUID.fromString(jwt.getSubject())));
    }
}
