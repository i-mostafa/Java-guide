# 14. Best practices, pitfalls & Spring Boot 4

## 14.1 Pull-request checklist

**Design**
- [ ] Controller thin; logic in a service; data access only in repositories.
- [ ] Request/response DTOs (records); entities never leave the service layer.
- [ ] Package-by-feature (or whatever the codebase uses — consistency beats preference).

**Correctness**
- [ ] Input validated (`@Valid` + constraints); business rules → 422 with a clear code.
- [ ] Money in `BigDecimal`, time in `java.time` with an injected `Clock`.
- [ ] `@Transactional` on service methods; no remote calls inside transactions; no self-invocation of proxied methods.
- [ ] Optimistic locking (`@Version`) on entities that can be updated concurrently.
- [ ] Kafka consumers idempotent; events published only after commit (or via outbox).
- [ ] New DB change = new Flyway migration (never edit an old one); indexes for new queries.

**Security**
- [ ] Endpoint has an explicit rule (`@PreAuthorize`) and an **ownership** check where relevant.
- [ ] No secrets or PII in code, config in git, logs, or error responses.
- [ ] `toString()` of records with sensitive fields overridden.

**Resilience & ops**
- [ ] Every outbound call has timeouts; retries only for idempotent operations; circuit breaker for external deps.
- [ ] Meaningful logs (ids, not PII; placeholders; exception as last arg); metrics for key business events.
- [ ] Config via `@ConfigurationProperties` with validation and env-var overrides.

**Tests**
- [ ] Unit tests for logic, slice/IT for wiring; happy path + main failure paths; no `Thread.sleep`.

## 14.2 Pitfalls for Node/TS developers

| Pitfall | Instead |
|---|---|
| `==` on Strings/objects | `.equals()` / `Objects.equals()` |
| `double` for money | `BigDecimal` + `compareTo` |
| Field injection `@Autowired private X x;` | constructor injection (final fields) |
| Storing request data in a singleton bean field | local variables / method params (beans are shared across threads) |
| `@Transactional` on a private method or calling it via `this` | public method on another bean, or `TransactionTemplate` |
| Returning JPA entities from controllers | DTOs + mapper |
| `ddl-auto: update` | Flyway + `validate` |
| `FetchType.EAGER` everywhere / N+1 | lazy + fetch joins / projections |
| Catch-and-ignore exceptions | handle, wrap with context, or let the advice handle it |
| Blocking calls in WebFlux / gateway | keep reactive code non-blocking |
| Trying to write `async/await`-style code in MVC | just write blocking code; virtual threads make it scale |
| `Optional` as a field/param type | only for return values |
| Mutating `List.of(...)` | `new ArrayList<>(List.of(...))` |
| Ignoring compiler warnings | treat MapStruct "unmapped property" warnings as bugs |
| Hard-coding URLs/keys | properties + env vars |

## 14.3 Spring Boot 4 / Spring Framework 7 — what changes

This repo targets **Spring Boot 3.5.x**, which many production codebases run. Spring Boot **4.0** (Nov 2025) and newer 4.x releases are the current generation; if your project is on 4.x (or migrating), these are the changes you'll notice most. **Always confirm details in the official migration guide** linked below — this is a summary, not a substitute.

| Area | Boot 3.5 (this repo) | Boot 4.x |
|---|---|---|
| Platform | Spring Framework 6.2, Jakarta EE 10 | Spring Framework 7, Jakarta EE 11 (Servlet 6.1); Java 17 baseline still, 21/25 recommended |
| Starters | `spring-boot-starter-web` | modularised; `spring-boot-starter-webmvc` (old names kept as deprecated aliases for a while), per-technology test starters (e.g. `spring-boot-starter-webmvc-test`) |
| JSON | Jackson 2 (`com.fasterxml.jackson.databind`) | **Jackson 3** by default (`tools.jackson.databind` packages; annotations stay in `com.fasterxml.jackson.annotation`); Jackson 2 support deprecated |
| Kafka JSON | `JsonSerializer` / `JsonDeserializer` | Jackson 3-based `JacksonJsonSerializer` / `JacksonJsonDeserializer` (old ones deprecated) in Spring Kafka 4 |
| Flyway/Liquibase | `flyway-core` enough | add `spring-boot-starter-flyway` (auto-config moved to its own module) |
| AOP starter | `spring-boot-starter-aop` | renamed `spring-boot-starter-aspectj` |
| Test mocks | `@MockitoBean` (`@MockBean` deprecated) | `@MockBean` removed — use `@MockitoBean` (this repo already does) |
| Resilience | Resilience4j | Framework 7 adds core `@Retryable` / `@ConcurrencyLimit` (with `@EnableResilientMethods`); Resilience4j still fine |
| HTTP clients | HTTP Interfaces wired by hand (`HttpServiceProxyFactory`) | `@ImportHttpServices` + `spring.http.client.service.*` config to register groups of HTTP interface clients |
| Null safety | — | JSpecify `@Nullable`/`@NullMarked` annotations across Spring APIs |
| API versioning | manual | first-class API versioning support in Spring MVC/WebFlux |
| Spring Cloud | 2025.0.x (Northfields) | 2025.1.x (Oakwood) |
| Removed | — | Undertow support, many long-deprecated APIs |

Migration approach: upgrade to the latest 3.5.x first and fix all deprecation warnings → run the **spring-boot-properties-migrator** dependency temporarily (reports renamed properties at startup) → consider **OpenRewrite** recipes (`org.openrewrite.java.spring.boot4.*`) to automate mechanical changes → bump to 4.x and the matching Spring Cloud train → run the full test suite.

## Sources
- Spring Boot 4.0 migration guide: <https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide>
- Spring Boot release notes (wiki): <https://github.com/spring-projects/spring-boot/wiki>
- Spring Framework 7 "What's new": <https://github.com/spring-projects/spring-framework/wiki/What%27s-New-in-Spring-Framework-7.x>
- Spring Boot support timeline: <https://spring.io/projects/spring-boot#support>
- OpenRewrite Spring recipes: <https://docs.openrewrite.org/recipes/java/spring>
