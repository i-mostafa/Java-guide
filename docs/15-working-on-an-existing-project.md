# 15. Working on an existing Spring Boot project — a playbook

## 15.1 Day 1: get oriented (in this order)

1. **Root `pom.xml`** (or `build.gradle`): Spring Boot version, Spring Cloud train, Java version, modules, notable libraries (MapStruct? Lombok? Feign? Avro?). This tells you which docs version to read.
2. **README / `docs/` / Confluence** for how to run locally, required env vars, and infra (docker compose? Testcontainers? shared dev DB?).
3. **`*Application.java`** — the root package, extra `@Enable...` annotations (`@EnableFeignClients`, `@EnableScheduling`, `@EnableCaching`).
4. **`src/main/resources/application*.yml`** — profiles, datasources, Kafka topics, external URLs, feature flags. Note what's overridden by env vars / config server / Kubernetes ConfigMaps.
5. **Package layout** — by feature or by layer? Hexagonal? Where do controllers, services, clients, listeners live?
6. **`config/` or `*Config` classes** — security rules, Kafka error handling, HTTP clients, Jackson customisation.
7. **Migrations** (`db/migration` or Liquibase changelogs) — the real data model.
8. **Tests** — the fastest documentation of expected behaviour; find a good existing IT and copy its setup.

## 15.2 Trace one request end to end

Pick an endpoint from the ticket and follow it:

`@RestController` method → `@Service` → repository / client / producer → migration for the table → Kafka listener that reacts → other service.

IntelliJ helpers: *Find usages* (⌥F7), *Go to implementation* (⌥⌘B), **Endpoints** tool window (Ultimate) lists every URL, *Structure* (⌘7), *Search everywhere* for a URL fragment, and set a breakpoint + debug a local run while you call the endpoint.

## 15.3 Run it locally

- Start infra (compose / Testcontainers dev app), then the service with the right profile (`local`/`dev`).
- If the service needs 10 others, check whether the team stubs them (WireMock), points at a shared dev environment, or uses feature toggles.
- First run: `./mvnw clean verify` to confirm your setup passes the same checks as CI.

## 15.4 Making your first change

1. Branch from the main branch following the team convention (`feature/ABC-123-short-name`).
2. Write/extend a test first when fixing a bug (reproduce it).
3. Follow existing patterns even if you'd do it differently — consistency wins; propose improvements separately.
4. Schema change → new migration file with the next version number.
5. New config → property with a sane default + env var, documented.
6. New endpoint → validation, security rule, error mapping, OpenAPI annotations, tests.
7. Run `./mvnw verify` locally; keep the PR small with a clear description (what/why/how tested).

## 15.5 Debugging recipes

| Problem | Look at |
|---|---|
| App won't start: "No qualifying bean of type X" | Is X annotated/in the scanned package? Missing `@Configuration`? Profile/condition not active? Start with `--debug` |
| "Failed to configure a DataSource" | Missing `spring.datasource.url` for the active profile |
| `LazyInitializationException` | Accessing a lazy relation outside a transaction → fetch it in the query or map to DTO inside the service |
| Annotation (`@Transactional`, `@Retry`) has no effect | Self-invocation / private method / class not a bean |
| 401 everywhere | Token issuer/audience/JWKS URL mismatch; clock skew; check the `WWW-Authenticate` header |
| 403 | Role names (`ROLE_` prefix!), method security expressions |
| Kafka messages not consumed | Group id, topic name, `auto-offset-reset`, deserialization errors (check the DLT), consumer lag in Kafka UI |
| JSON field missing / wrong | Jackson naming, record accessor, `@JsonIgnoreProperties`, Boot `spring.jackson.*` settings |
| Slow endpoint | Enable SQL logging (N+1?), check traces in Jaeger/Tempo, Hikari pool metrics |
| Dependency conflict (`NoSuchMethodError`) | `mvn dependency:tree -Dincludes=groupId:artifactId`; don't override versions managed by the Boot BOM without reason |

## 15.6 How this maps to a TypeScript background

You already know HTTP, REST design, async messaging, SQL, Docker and testing — that's most of the job. The new parts are the **JVM toolchain** (Maven, IntelliJ), the **Spring container** (DI, proxies, auto-configuration) and **JPA's persistence context**. Chapters 3, 4 and 6 are where to invest your first week.

## Sources
- Spring Boot "How-to" guides (answers to most day-to-day questions): <https://docs.spring.io/spring-boot/how-to/index.html>
- Spring guides (short, task-focused tutorials): <https://spring.io/guides>
- Baeldung Spring tutorials: <https://www.baeldung.com/spring-tutorial>
