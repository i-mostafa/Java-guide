# 5. REST APIs, validation & error handling

## 5.1 Controllers

```java
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    @GetMapping("/me")                                    // GET /api/customers/me
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerResponse me(@AuthenticationPrincipal Jwt jwt) { ... }

    @PutMapping("/me")
    public CustomerResponse update(@AuthenticationPrincipal Jwt jwt,
                                   @Valid @RequestBody UpdateProfileRequest request) { ... }

    @GetMapping("/{id}")
    public CustomerResponse byId(@PathVariable UUID id) { ... }       // "abc" → 400 automatically

    @GetMapping
    public PageResponse<CustomerResponse> search(@RequestParam(required = false) KycStatus kycStatus,
                                                 Pageable pageable) { ... }
}
```

| Express | Spring MVC |
|---|---|
| `req.params.id` | `@PathVariable UUID id` (typed + converted) |
| `req.query.status` | `@RequestParam KycStatus status` |
| `req.body` | `@RequestBody Dto body` (JSON → object by **Jackson**) |
| `req.headers['x']` | `@RequestHeader("X") String x` |
| `res.status(201).json(x)` | `ResponseEntity.created(uri).body(x)` or `@ResponseStatus(HttpStatus.CREATED)` |
| `req.user` | `@AuthenticationPrincipal Jwt jwt` |

**Status codes** used in this repo: 200 read/update · 201 + `Location` create (`FinanceApplicationController.submit`) · 204 no content · 400 validation · 401 no/invalid token · 403 not allowed · 404 not found · 409 conflict/duplicate/concurrent update · 422 business rule violated · 503 dependency down.

**Keep controllers thin:** parse → call service → map to response DTO. No business logic, no repositories.

## 5.2 DTOs — never expose entities

Separate request DTOs, response DTOs and entities:
- entities change with the DB; API contracts must stay stable,
- entities can contain secrets (`passwordHash`) or lazy relations (serialization explodes),
- request DTOs define exactly what a client may set (no mass-assignment of `role`!).

Use **records** for DTOs and **MapStruct** for entity → DTO mapping (`CustomerMapper`, generated at compile time; unmapped target fields produce warnings, which you can turn into errors with `unmappedTargetPolicy = ReportingPolicy.ERROR`). For updates prefer domain methods (`customer.updateProfile(...)`) over blind field copying; MapStruct's `@MappingTarget` is the alternative.

## 5.3 Bean Validation (Jakarta Validation)

Add `spring-boot-starter-validation`, annotate DTO fields, and put `@Valid` on the parameter:

```java
public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "must be in E.164 format") String phoneNumber,
        @NotNull @Past @Adult LocalDate dateOfBirth,      // @Adult = our custom constraint
        @NotBlank @Pattern(regexp = "^[0-9A-Z-]{6,20}$") String nationalId) {}
```

Common constraints: `@NotNull` (not null), `@NotEmpty` (not null/empty), `@NotBlank` (strings: not just whitespace), `@Size`, `@Min/@Max`, `@DecimalMin`, `@Digits`, `@Positive`, `@Email`, `@Pattern`, `@Past/@Future`, `@AssertTrue`. Nested objects need `@Valid` on the field too.

Three ways to write custom rules (all in this repo):

1. **Custom field constraint** — `@Adult` + `AdultValidator` (customer-service).
2. **Cross-field class-level constraint** — `@ValidFinanceRatio` + `FinanceRatioValidator`, attaching the error to `financeAmount` (application-service).
3. **Quick cross-field check** — an `@AssertTrue` method on the record (`UpdateStatusRequest.isReasonPresentWhenRejected`).

Other tips:
- Validate path/query params by putting `@Validated` on the controller class and constraints on parameters (`@PathVariable @Min(1) long id`).
- **Validation groups** (`groups = OnCreate.class`) let one DTO have different rules per operation — use sparingly; separate DTOs are clearer.
- Validation checks *shape*; **business rules** (KYC verified? FTV ≤ 80 % of the *valuation*?) belong in the service and produce 422 (`BusinessRuleException`).
- `@ConfigurationProperties` records are validated the same way.

## 5.4 Centralised errors with ProblemDetail (RFC 9457)

`common-lib`'s `GlobalExceptionHandler` (`@RestControllerAdvice extends ResponseEntityExceptionHandler`) turns every exception into the standard `application/problem+json`:

```json
{
  "type": "https://homefin.example/problems/validation-failed",
  "title": "validation-failed",
  "status": 400,
  "detail": "Request validation failed",
  "traceId": "6f1c0e...",
  "errors": [ { "field": "phoneNumber", "message": "must be in E.164 format, e.g. +971501234567" } ]
}
```

Pattern:
- throw meaningful exceptions from services (`NotFoundException`, `ConflictException`, `BusinessRuleException`, `DownstreamServiceException`),
- map them in **one** place,
- log 5xx with stack traces, 4xx at INFO without,
- never leak stack traces or SQL to clients,
- include a `traceId` so support can find the logs.

## 5.5 Pagination & sorting

`Pageable` is resolved from `?page=0&size=20&sort=createdAt,desc`; defaults via `@PageableDefault`; cap with `spring.data.web.pageable.max-page-size`. Return your own stable DTO (`PageResponse`) instead of Spring's `Page` JSON. For huge tables or infinite scroll use keyset ("seek") pagination (`WHERE created_at < :cursor`) instead of offsets.

## 5.6 API documentation — OpenAPI / Swagger

`springdoc-openapi-starter-webmvc-ui` generates the spec from your code: `/v3/api-docs` (JSON) and `/swagger-ui.html`. Enrich with `@Operation`, `@Schema(example = ...)`, and `@SecurityScheme` (see `OpenApiConfig`). Many teams go **contract-first** instead: write `openapi.yaml`, generate interfaces with `openapi-generator-maven-plugin`, implement them.

## 5.7 Other REST best practices

- Nouns + HTTP verbs, plural collections (`/api/applications/{id}/status`).
- **Version** your API (`/api/v1/...` or a header) before the first external client.
- Idempotency: `PUT`/`DELETE` are idempotent; for retry-safe `POST`s accept an `Idempotency-Key` header (we *send* one to the KYC provider).
- Optimistic locking surfaces as 409 (`@Version` → `OptimisticLockingFailureException`); expose `ETag`/`If-Match` for public APIs.
- Keep JSON conventions consistent (camelCase, ISO-8601 dates — Boot's default Jackson config writes `Instant` as `"2026-09-29T10:15:30Z"`).

## Sources
- Spring MVC annotated controllers: <https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html>
- Error responses / ProblemDetail: <https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html>
- RFC 9457 Problem Details: <https://www.rfc-editor.org/rfc/rfc9457>
- Validation in Spring: <https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html>
- Hibernate Validator reference (custom constraints): <https://docs.jboss.org/hibernate/validator/8.0/reference/en-US/html_single/>
- MapStruct reference: <https://mapstruct.org/documentation/stable/reference/html/>
- springdoc-openapi: <https://springdoc.org>
