# 13. Docker & deployment

## 13.1 Building images

Two good options:

**A. Dockerfile (this repo).** One multi-stage [`Dockerfile`](../Dockerfile) for every module:
1. `maven:3.9-eclipse-temurin-21` builds `-pl <module> -am` with a BuildKit cache for `~/.m2`,
2. `java -Djarmode=tools -jar app.jar extract --layers --launcher` splits the fat jar into layers (dependencies change rarely → cached),
3. a slim `eclipse-temurin:21-jre` runtime as a **non-root** user, with `-XX:MaxRAMPercentage=75` so the heap respects container limits.

```bash
docker build --build-arg MODULE=customer-service -t homefin/customer-service .
```

**B. Cloud Native Buildpacks** — no Dockerfile at all:

```bash
mvn -pl customer-service spring-boot:build-image -Dspring-boot.build-image.imageName=homefin/customer-service
```

Other options you'll meet: Jib (`jib-maven-plugin`), GraalVM native images (`-Pnative`, fast startup, longer builds, reflection caveats).

## 13.2 docker compose

`docker-compose.yml` has two layers:
- default: infra (Postgres with one DB per service, Kafka in KRaft mode, Kafka UI, WireMock, Jaeger),
- profile `apps`: all Spring services with `SPRING_PROFILES_ACTIVE=docker`.

Note the two Kafka listeners: containers use `kafka:19092`, your laptop uses `localhost:9092`.

Spring Boot can also start compose for you on `spring-boot:run` via `spring-boot-docker-compose` — handy in single-service repos.

## 13.3 Running on Kubernetes (what the code already supports)

| Concern | In this repo |
|---|---|
| Liveness/readiness probes | `management.endpoint.health.probes.enabled: true` → `/actuator/health/liveness`, `/readiness` |
| Graceful shutdown | `server.shutdown: graceful` (+ `spring.lifecycle.timeout-per-shutdown-phase`) — finishes in-flight requests on SIGTERM |
| Config via env | every environment-specific value has an env var placeholder |
| Stateless instances | no session state; JWT auth |
| Horizontal scaling | Kafka consumer groups split partitions; DB migrations are safe to run from several pods (Flyway locks) |
| Logs to stdout | yes, JSON in `docker` profile |
| Metrics | `/actuator/prometheus` |
| Behind a proxy | `server.forward-headers-strategy: framework` |

Typical Deployment snippet:

```yaml
readinessProbe: { httpGet: { path: /actuator/health/readiness, port: 8082 }, initialDelaySeconds: 10 }
livenessProbe:  { httpGet: { path: /actuator/health/liveness,  port: 8082 }, initialDelaySeconds: 30 }
resources: { requests: { cpu: 250m, memory: 512Mi }, limits: { memory: 768Mi } }
env:
  - { name: SPRING_PROFILES_ACTIVE, value: prod }
  - { name: DB_PASSWORD, valueFrom: { secretKeyRef: { name: customer-db, key: password } } }
```

## 13.4 12-factor checklist

Config in the environment · backing services as attached resources (URLs) · stateless processes · port binding (embedded Tomcat/Netty) · disposability (fast start, graceful stop) · dev/prod parity (same Postgres/Kafka in Testcontainers) · logs as event streams.

## 13.5 CI pipeline (typical)

`mvn -B verify` (unit + IT with Testcontainers; CI runner needs Docker) → static analysis (SonarQube, SpotBugs, Checkstyle/Spotless formatting) → dependency scan (OWASP Dependency-Check/Snyk/Trivy for images) → build & push image → deploy (Helm/Argo CD).

## Sources
- Spring Boot container images: <https://docs.spring.io/spring-boot/reference/packaging/container-images/index.html>
- Efficient container images (layers, jarmode tools): <https://docs.spring.io/spring-boot/reference/packaging/container-images/efficient-images.html>
- Kubernetes probes in Boot: <https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.kubernetes-probes>
- Graceful shutdown: <https://docs.spring.io/spring-boot/reference/web/graceful-shutdown.html>
- The Twelve-Factor App: <https://12factor.net>
