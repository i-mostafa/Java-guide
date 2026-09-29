package com.homefin.customer.customer;

import com.homefin.common.web.PageResponse;
import com.homefin.customer.customer.dto.CustomerResponse;
import com.homefin.customer.customer.dto.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST endpoints for customer profiles: the equivalent of an Express router / a NestJS {@code @Controller()} class.
 * It stays thin: read input, call {@link CustomerService}, map the entity to a DTO with {@link CustomerMapper}.
 *
 * <p>Who calls these methods? Spring MVC, once per matching HTTP request, on a Tomcat worker thread. Before that,
 * the Spring Security filter chain (SecurityConfig) has already validated the JWT. The returned object is
 * serialized to JSON by Jackson with status 200. Thrown exceptions are turned into error responses by
 * common-lib's GlobalExceptionHandler (like a NestJS exception filter).
 *
 * <ul>
 *   <li>{@code @RestController} (runtime): a bean whose methods handle HTTP requests and whose return values are
 *       written as the response body (JSON), rather than rendered as HTML views.</li>
 *   <li>{@code @RequestMapping("/api/customers")} (runtime): base path for every method in the class, like
 *       {@code app.use('/api/customers', router)}.</li>
 *   <li>{@code @RequiredArgsConstructor} (Lombok, compile time): generates the constructor for the final fields;
 *       Spring injects the service and mapper beans through it.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerMapper mapper;

    // @Operation (runtime, springdoc): summary text shown in Swagger UI. Has no effect on behavior.
    // @GetMapping("/me") (runtime): handle GET /api/customers/me, like router.get('/me', handler).
    // @PreAuthorize (runtime, AOP): evaluated before the method runs; if the JWT lacks role CUSTOMER -> 403.
    //   Enabled by @EnableMethodSecurity in SecurityConfig. hasRole('CUSTOMER') checks for authority ROLE_CUSTOMER.
    // @AuthenticationPrincipal (runtime): inject the authenticated principal, here the decoded, already-verified
    //   JWT (like req.user after passport-jwt).
    @Operation(summary = "Get my customer profile")
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerResponse me(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(customerService.getByUserId(userId(jwt)));
    }

    // @PutMapping: handle PUT /api/customers/me.
    // @RequestBody (runtime): parse the JSON body into UpdateProfileRequest (like req.body, but typed).
    // @Valid (runtime): run the Bean Validation annotations on that record first; failures -> 400, method not called.
    @Operation(summary = "Update my profile")
    @PutMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return mapper.toResponse(customerService.updateProfile(userId(jwt), request));
    }

    // @PostMapping: handle POST /api/customers/me/kyc (a command endpoint with no request body).
    @Operation(summary = "Run identity verification (KYC) with the 3rd-party provider")
    @PostMapping("/me/kyc")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerResponse verifyKyc(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(customerService.verifyKyc(userId(jwt)));
    }

    // @GetMapping with no path = GET /api/customers itself.
    // @RequestParam(required = false) (runtime): bind query param ?kycStatus=PENDING (converted to the enum
    //   automatically; an unknown value -> 400). Missing -> null.
    // @PageableDefault (runtime): Spring builds a Pageable from ?page=&size=&sort= and uses these defaults when
    //   they're absent. max-page-size in application.yml caps "size".
    // PageResponse<CustomerResponse> is a generic type: a page wrapper whose items are CustomerResponse.
    @Operation(summary = "[Admin] Search customers, e.g. ?kycStatus=PENDING&page=0&size=20&sort=createdAt,desc")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<CustomerResponse> search(
            @RequestParam(required = false) KycStatus kycStatus,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        // mapper::toResponse is a METHOD REFERENCE, shorthand for the lambda c -> mapper.toResponse(c).
        // It's passed as a function to map each entity on the page to a DTO, like page.items.map(mapper.toResponse).
        return PageResponse.of(customerService.search(kycStatus, pageable), mapper::toResponse);
    }

    // "{id}" is a path variable, like "/:id" in Express.
    // @PathVariable (runtime): bind it to the parameter of the same name, converting the text to a UUID
    //   (an invalid UUID -> 400 before the method runs).
    @Operation(summary = "[Admin] Get a customer by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerResponse byId(@PathVariable UUID id) {
        return mapper.toResponse(customerService.getById(id));
    }

    // private static helper: only usable inside this class and needs no instance state.
    // The JWT "sub" (subject) claim holds the auth-service user id as a string.
    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
