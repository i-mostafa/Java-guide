# HomeFin Platform — Spring Boot Microservices Reference

A complete, runnable **reference project** for engineers moving from **TypeScript / Node.js** to **Java + Spring Boot**.
It models a small *property finance* platform and deliberately touches every technology you meet in a real
enterprise Spring codebase: REST, validation, JWT security, PostgreSQL/JPA/Flyway, Kafka, service discovery,
central config, an API gateway, 3rd-party HTTP calls with resilience, structured logging, tracing, metrics,
Docker, and a full test pyramid.

> 📘 **The learning guide lives in [`docs/`](docs/README.md).** Every chapter points back to the code in this repo.

---

## 1. Architecture

```
                         ┌──────────────────────┐
  client ──HTTP──▶       │  api-gateway :8080   │  JWT check, routing (lb://), request logging
                         └─────────┬────────────┘
                 ┌─────────────────┼──────────────────────┐
                 ▼                 ▼                      ▼
        ┌──────────────┐  ┌──────────────────┐   ┌──────────────────────┐
        │ auth-service │  │ customer-service │◀──│ application-service  │  (OpenFeign + token relay)
        │    :8081     │  │      :8082       │   │        :8083         │
        └──────┬───────┘  └───┬─────────┬────┘   └───┬───────────┬──────┘
               │ authdb       │ custdb  │ HTTP       │ appdb     │ HTTP
               ▼              ▼         ▼            ▼           ▼
          PostgreSQL (database-per-service)   KYC provider   Valuation provider   (WireMock = fake 3rd parties)

   Kafka topics
     homefin.auth.user-registered.v1          auth-service ──▶ customer-service (creates profile)
     homefin.application.submitted.v1         application-service ──▶ (any interested consumer)
     homefin.application.status-changed.v1    application-service ──▶ customer-service (notifies)

   Platform:  config-server :8888 (central config)   discovery-server :8761 (Eureka)   Jaeger :16686 (traces)
```

| Module | What it demonstrates |
|---|---|
| `common-lib` | Shared library: RFC 9457 error handling, JWT role mapping, event contracts, `BaseEntity`, a **custom auto-configuration** |
| `config-server` | Spring Cloud Config (native backend) serving shared + per-profile config |
| `discovery-server` | Eureka service registry |
| `api-gateway` | Spring Cloud Gateway (reactive/WebFlux), edge JWT validation, global filter |
| `auth-service` | Registration/login, BCrypt, **issuing RS256 JWTs**, JWKS endpoint, publish Kafka event after commit |
| `customer-service` | Resource server, `@PreAuthorize`, validation incl. custom constraint, MapStruct, Kafka consumers + retry/DLT, **3rd-party KYC** via `RestClient` + Resilience4j |
| `application-service` | OpenFeign service-to-service call, **HTTP Interface** client, cross-field validation, state machine, `TransactionTemplate`, `@PostAuthorize`, Kafka producer |

## 2. Tech stack

Java 21 · Spring Boot 3.5 · Spring Cloud 2025.0 · Spring Security (OAuth2 Resource Server + Nimbus JOSE) ·
Spring Data JPA / Hibernate 6 · PostgreSQL 17 · Flyway · Spring for Apache Kafka · OpenFeign · RestClient / HTTP Interfaces ·
Resilience4j · MapStruct · Lombok · springdoc-openapi · Micrometer + OpenTelemetry · JUnit 5 · Mockito · AssertJ · Testcontainers · Docker Compose

> Versions are pinned in the root [`pom.xml`](pom.xml). Check <https://start.spring.io> and
> <https://spring.io/projects/spring-boot#support> for the newest versions before starting a new project,
> and see [docs/14-best-practices-and-boot-4.md](docs/14-best-practices-and-boot-4.md) for the Spring Boot 4 upgrade notes.

## 3. Prerequisites (install once)

| Tool | Why | Install (macOS) |
|---|---|---|
| **JDK 21** (Temurin) | compile & run Java | `curl -s "https://get.sdkman.io" \| bash` then `sdk env install` (reads `.sdkmanrc`) — or `brew install --cask temurin@21` |
| **Maven 3.9+** | build tool (the "npm" of Java) | `sdk install maven` or `brew install maven` |
| **Docker Desktop** (or OrbStack / Colima) | Postgres, Kafka, WireMock, Jaeger, Testcontainers | <https://www.docker.com/products/docker-desktop/> |
| **IntelliJ IDEA** (Community is enough; Ultimate has Spring tooling) | IDE | `brew install --cask intellij-idea-ce` |
| *Optional:* `jq`, `httpie`, DBeaver, `kcat` | poke APIs / DB / Kafka | `brew install jq httpie kcat` · `brew install --cask dbeaver-community` |

Full explanation and IDE setup: [docs/01-tooling-setup.md](docs/01-tooling-setup.md).

Verify:

```bash
java -version      # openjdk version "21..."
mvn -v             # Apache Maven 3.9.x, Java version: 21...
docker compose version
```

*(Optional)* generate the Maven Wrapper so teammates don't need Maven installed: `mvn -N wrapper:wrapper`, then use `./mvnw` everywhere.

## 4. Quick start

### Option A — everything in Docker (easiest)

```bash
cp .env.example .env                              # local-only secrets/defaults
docker compose --profile apps up -d --build       # first build takes a few minutes
docker compose ps
```

### Option B — infrastructure in Docker, services from your IDE (best for development/debugging)

```bash
docker compose up -d                              # postgres, kafka, kafka-ui, wiremock, jaeger
mvn clean install -DskipTests                     # build all modules once (installs common-lib locally)
```

Then start, **in this order**, each `*Application` class from IntelliJ (▶ next to `main`) — or from a terminal:

```bash
mvn -pl config-server       spring-boot:run
mvn -pl discovery-server    spring-boot:run
mvn -pl auth-service        spring-boot:run
mvn -pl customer-service    spring-boot:run
mvn -pl application-service spring-boot:run
mvn -pl api-gateway         spring-boot:run
```

### Try it

Open [`http/homefin.http`](http/homefin.http) in IntelliJ (or VS Code + *REST Client*) and run the requests top to bottom:
register → login → profile → KYC → submit application → admin review. Or with curl:

```bash
curl -s localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"jane@example.com","password":"S3cure#Passw0rd","firstName":"Jane","lastName":"Doe"}' | jq

TOKEN=$(curl -s localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"jane@example.com","password":"S3cure#Passw0rd"}' | jq -r .accessToken)

curl -s localhost:8080/api/customers/me -H "Authorization: Bearer $TOKEN" | jq
```

Default admin (dev only): `admin@homefin.local` / `Admin#12345`.

**Fake 3rd-party behaviour (WireMock):** `nationalId` starting with `000` → KYC *REJECTED*, `999` → provider outage (watch retries + circuit breaker);
`propertyReference` starting with `UNKNOWN` → 404, `SLOW` → timeout.

## 5. Useful URLs

| URL | What |
|---|---|
| <http://localhost:8080> | API gateway (use this for all API calls) |
| <http://localhost:8081/swagger-ui.html> · `:8082` · `:8083` | Swagger UI per service |
| <http://localhost:8761> | Eureka dashboard (registered instances) |
| <http://localhost:8888/customer-service/docker> | What config-server serves to customer-service in profile `docker` |
| <http://localhost:8090> | Kafka UI (topics, messages, consumer lag, DLTs) |
| <http://localhost:16686> | Jaeger — follow one request across gateway → services → Kafka |
| <http://localhost:8082/actuator/health> · `/actuator/prometheus` · `/actuator/circuitbreakers` | Health, metrics, breaker state |
| <http://localhost:9090/__admin/mappings> | WireMock stubs |
| `localhost:5432` (homefin/homefin) | PostgreSQL (`authdb`, `customerdb`, `applicationdb`) |

## 6. Build & test

```bash
mvn test                        # unit + slice tests (fast, no Docker needed)
mvn verify                      # + integration tests *IT (Testcontainers → Docker must be running)
mvn -pl customer-service -am verify          # one module (+ what it depends on)
mvn -pl customer-service test -Dtest=CustomerServiceTest   # one test class
mvn -pl auth-service spring-boot:run -Dspring-boot.run.profiles=dev   # run with a profile
```

## 7. Project layout

```
homefin-platform/
├── pom.xml                      # parent POM: versions, plugins, module list
├── common-lib/                  # shared code + custom auto-configuration
├── config-server/               # src/main/resources/config-repo/*.yml = the central config
├── discovery-server/
├── api-gateway/
├── auth-service/                # each service: package-by-feature
│   └── src/main/java/com/homefin/auth/
│       ├── AuthServiceApplication.java
│       ├── config/              # @Configuration classes, @ConfigurationProperties
│       ├── auth/                # controller, service, dto/
│       ├── user/                # entity, repository
│       └── events/              # domain events -> Kafka
├── customer-service/
├── application-service/
├── infra/                       # postgres init, wiremock stubs
├── http/homefin.http            # runnable API walkthrough
├── docs/                        # THE GUIDE
├── Dockerfile                   # one multi-stage Dockerfile for every module
└── docker-compose.yml
```

## 8. Troubleshooting

| Symptom | Fix |
|---|---|
| `Connection refused localhost:5432/9092` | `docker compose up -d` and wait for `docker compose ps` to show healthy |
| Service starts but calls through the gateway return 503 | The service isn't registered yet — check <http://localhost:8761>, wait ~30 s |
| 401 after restarting auth-service | Dev signing key is regenerated on each start — log in again |
| `/api/customers/me` → 404 right after registering | Profile is created asynchronously from Kafka; retry after a second (eventual consistency) |
| `Could not resolve dependencies ... common-lib` | Run `mvn install -DskipTests` from the root once (or use `-am`) |
| Lombok/MapStruct symbols "cannot be found" in IntelliJ | Enable *Settings → Build → Compiler → Annotation Processors → Enable annotation processing* |
| Port already in use | `lsof -i :8081` and kill the process, or change `server.port` |
| `Error processing condition on ...resilience4j...FallbackConfigurationOnMissingBean` | Mixed Resilience4j versions on the classpath — check `mvn -pl customer-service dependency:tree -Dincludes=io.github.resilience4j`; all must share one version (the root pom imports `resilience4j-bom` for this) |
| Testcontainers can't find Docker (Colima/OrbStack) | see <https://java.testcontainers.org/supported_docker_environment/> |

## 9. What is intentionally simplified

The code is production-*style*, but a few things are simplified for local development and called out in comments and the guide:
ephemeral JWT signing key (use a KMS/Vault or a real authorization server such as Keycloak / Spring Authorization Server),
no refresh tokens, publish-after-commit instead of a transactional outbox, config-server without git backend/encryption,
no rate limiting at the gateway, WireMock instead of real providers.
