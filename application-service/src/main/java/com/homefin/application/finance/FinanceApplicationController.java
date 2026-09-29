package com.homefin.application.finance;

import com.homefin.application.finance.dto.ApplicationResponse;
import com.homefin.application.finance.dto.CreateApplicationRequest;
import com.homefin.application.finance.dto.UpdateStatusRequest;
import com.homefin.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

/**
 * REST endpoints for finance applications (base path /api/applications).
 *
 * <p>Role: the HTTP layer only. It extracts input (path variables, query params, JSON body, the JWT),
 * checks roles, delegates to FinanceApplicationService and maps entities to DTOs. Spring calls these
 * methods once per matching HTTP request, after the security filter chain has validated the JWT.
 * Return values are serialized to JSON by Jackson; exceptions are turned into RFC 7807 problem responses
 * by common-lib's GlobalExceptionHandler (like a NestJS exception filter).
 *
 * <p>TS analogy: a NestJS {@code @Controller('api/applications')} with {@code @Get()}, {@code @Post()},
 * {@code @Body()}, {@code @Param()} decorators and {@code @UseGuards(RolesGuard)}, or an Express router.
 */
// @RestController (runtime): a bean whose methods handle HTTP requests and whose return values are written
// as the response body (JSON), not rendered as HTML views.
@RestController
// @RequestMapping (runtime): URL prefix shared by every endpoint in this class, like express.Router() mounted
// at "/api/applications".
@RequestMapping("/api/applications")
// @RequiredArgsConstructor (Lombok, compile time): constructor for the two final fields; Spring injects them.
@RequiredArgsConstructor
public class FinanceApplicationController {

    private final FinanceApplicationService service;
    private final ApplicationMapper mapper;

    // @Operation (runtime, springdoc): human description for the Swagger UI.
    // @PostMapping (runtime): handles POST /api/applications.
    // @PreAuthorize (runtime, Spring Security via AOP proxy): evaluates this SpEL (Spring Expression Language)
    // expression BEFORE the method runs; false -> 403. hasRole('CUSTOMER') checks for authority "ROLE_CUSTOMER".
    @Operation(summary = "Submit a property finance application")
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    // ResponseEntity<T> = body of type T plus full control over status code and headers (like res.status().set()).
    // @AuthenticationPrincipal (runtime): inject the authenticated principal, here the decoded JWT (like req.user).
    // @Valid (runtime): run Bean Validation on the argument; failures -> 400 before the method body executes.
    // @RequestBody (runtime): deserialize the JSON request body into the record (like @Body() in NestJS).
    public ResponseEntity<ApplicationResponse> submit(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody CreateApplicationRequest request) {
        // JWT "sub" claim = the user id, parsed from String into a java.util.UUID.
        FinanceApplication app = service.submit(UUID.fromString(jwt.getSubject()), request);
        // var = local type inference (the compiler infers java.net.URI), like `const location = ...` in TS.
        // Builds ".../api/applications/{id}" from the current request URL.
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(app.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(app)); // 201 + Location header
    }

    // @GetMapping (runtime): handles GET /api/applications.
    // @PageableDefault (runtime): builds a Pageable (page number, size, sort) from ?page=&size=&sort= query params,
    // with these defaults when they are absent. Spring Data turns it into LIMIT/OFFSET/ORDER BY.
    // Sort.Direction.DESC: an enum nested inside the Sort class.
    @Operation(summary = "List my applications")
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public PageResponse<ApplicationResponse> mine(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        // mapper::toResponse is a METHOD REFERENCE: shorthand for the lambda `app -> mapper.toResponse(app)`,
        // passed as the mapping function (like .map(mapper.toResponse) in JS, but with `this` bound correctly).
        return PageResponse.of(service.listForCustomer(UUID.fromString(jwt.getSubject()), pageable),
                mapper::toResponse);
    }

    /**
     * {@code @PostAuthorize}: load first, then check ownership on the returned object (authentication.name = JWT sub).
     *
     * <p>The method runs, then Spring evaluates the SpEL expression with {@code returnObject} bound to the return
     * value and {@code authentication} to the current user; if it is false the response becomes 403 (so another
     * customer can't read your application). Use it when the rule depends on data you only have after loading.
     */
    @Operation(summary = "Get one application (owner or admin)")
    // "/{id}" = a path variable, like "/:id" in Express.
    @GetMapping("/{id}")
    @PostAuthorize("hasRole('ADMIN') or returnObject.customerUserId().toString() == authentication.name")
    // @PathVariable (runtime): bind the {id} segment to the parameter (like req.params.id), converted to UUID;
    // an invalid UUID string results in a 400.
    public ApplicationResponse get(@PathVariable UUID id) {
        return mapper.toResponse(service.get(id));
    }

    @Operation(summary = "[Admin] List applications by status")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<ApplicationResponse> all(
            // @RequestParam (runtime): bind ?status=... (like req.query.status), converted to the enum.
            // required = false: may be absent, then the value is null (= no filter).
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(service.listByStatus(status, pageable), mapper::toResponse);
    }

    // @PatchMapping (runtime): handles PATCH /api/applications/{id}/status.
    @Operation(summary = "[Admin] Move an application to a new status")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApplicationResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) {
        return mapper.toResponse(service.changeStatus(id, request));
    }
}
