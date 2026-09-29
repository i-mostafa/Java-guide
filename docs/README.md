# Java & Spring Boot Guide for TypeScript / Node.js Engineers

This guide takes you from "I know TypeScript and Express/NestJS" to "I can contribute confidently to an
enterprise Spring Boot microservices codebase". Every concept is tied to **real code in this repository**,
so read a chapter, then open the referenced files.

> **How to use it:** skim chapters 1–4 in order (they're the foundation), then jump to whatever your task needs.
> Chapter 15 is a playbook for your first days on an existing project.
>
> **Read the code alongside the guide:** every Java, YAML and `pom.xml` file in this repo carries beginner-level
> comments with TypeScript/Node analogies. A good reading order for one feature end to end:
> `CustomerController` → `CustomerService` → `Customer` (entity) → `CustomerRepository` → `KycClient` →
> `UserRegisteredListener`, then the matching tests. The React UI in `frontend/` shows how each screen calls these endpoints.

| # | Chapter | You'll learn |
|---|---|---|
| 1 | [Tooling setup](01-tooling-setup.md) | JDK, Maven, IntelliJ, Docker — what to install and how to configure it |
| 2 | [Java for TypeScript developers](02-java-for-typescript-devs.md) | The language side by side with TS: types, classes, records, generics, null, collections, streams, exceptions, concurrency |
| 3 | [Maven & project structure](03-maven-and-project-structure.md) | `pom.xml` vs `package.json`, lifecycle, scopes, BOMs, multi-module builds, package-by-feature |
| 4 | [Spring core concepts](04-spring-core.md) | Dependency injection, beans, configuration, profiles, auto-configuration, proxies/AOP |
| 5 | [REST APIs, validation & errors](05-rest-apis-and-validation.md) | Controllers, DTOs, Bean Validation, custom validators, ProblemDetail, pagination, OpenAPI |
| 6 | [Data: JPA, transactions & Flyway](06-data-jpa-and-flyway.md) | Entities, repositories, transactions, migrations, N+1, optimistic locking |
| 7 | [Security & JWT](07-security-jwt.md) | Filter chain, issuing/validating JWTs, roles, method security, token relay |
| 8 | [Microservices with Spring Cloud](08-microservices-spring-cloud.md) | Gateway, config server, discovery, Feign, patterns |
| 9 | [Kafka](09-kafka.md) | Producers, consumers, keys & ordering, idempotency, retries & DLT, outbox |
| 10 | [3rd-party APIs & resilience](10-third-party-apis-and-resilience.md) | RestClient, HTTP interfaces, timeouts, retry, circuit breaker, WireMock |
| 11 | [Logging & observability](11-logging-and-observability.md) | SLF4J/Logback, MDC, structured logs, tracing, metrics, health |
| 12 | [Testing](12-testing.md) | JUnit 5, Mockito, AssertJ, slices, Testcontainers |
| 13 | [Docker & deployment](13-docker-and-deployment.md) | Images, compose, Kubernetes probes, graceful shutdown, 12-factor |
| 14 | [Best practices, pitfalls & Spring Boot 4](14-best-practices-and-boot-4.md) | Review checklist, Node-dev pitfalls, upgrade notes |
| 15 | [Working on an existing Spring project](15-working-on-an-existing-project.md) | How to find your way around, debug, and ship your first change |
| — | [References & further reading](references.md) | Official docs, books, articles |

## Mental model in one table

| Node / TS world | Spring world | In this repo |
|---|---|---|
| `package.json` + npm | `pom.xml` + Maven | [`pom.xml`](../pom.xml) |
| Express `app` / Nest module | Spring `ApplicationContext` | `*Application.java` |
| Express router / Nest controller | `@RestController` | `CustomerController` |
| Nest provider / manual `new Service(repo)` | `@Service` bean + constructor injection | `CustomerService` |
| middleware | Servlet `Filter` / Spring Security filter chain / `HandlerInterceptor` | `SecurityConfig`, gateway `RequestLoggingFilter` |
| error middleware | `@RestControllerAdvice` | `GlobalExceptionHandler` |
| zod / class-validator | Jakarta Bean Validation (`@NotBlank`, `@Valid`) | `UpdateProfileRequest` |
| TypeORM / Prisma | Spring Data JPA + Hibernate | `CustomerRepository` |
| knex / prisma migrate | Flyway | `db/migration/V1__*.sql` |
| `process.env` + dotenv | `application.yml` + `@ConfigurationProperties` + profiles | `KycProperties` |
| passport-jwt / jose | Spring Security OAuth2 Resource Server | `SecurityConfig` |
| axios / fetch | `RestClient`, HTTP Interfaces, OpenFeign | `KycClient`, `ValuationClient`, `CustomerClient` |
| kafkajs | Spring for Apache Kafka | `UserRegisteredListener` |
| opossum / cockatiel | Resilience4j | `@CircuitBreaker` in `KycClient` |
| pino / winston | SLF4J + Logback | `@Slf4j` |
| jest | JUnit 5 + Mockito + AssertJ | `src/test/java` |
| supertest | MockMvc | `CustomerControllerTest` |
| nodemon | spring-boot-devtools | service `pom.xml` |
