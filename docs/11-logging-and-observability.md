# 11. Logging & observability

## 11.1 Logging stack

**SLF4J** is the API you code against; **Logback** is Boot's default implementation. Lombok's `@Slf4j` gives you `log`.

```java
log.info("Customer profile created for userId={}", event.userId());     // {} placeholders — NOT string concatenation
log.warn("KYC provider unavailable: {}", t.toString());
log.error("Unexpected error", ex);                                        // exception as LAST arg → full stack trace
if (log.isDebugEnabled()) { log.debug("Big payload {}", expensive()); }  // guard only expensive computations
```

| Level | Use for |
|---|---|
| ERROR | something failed and needs attention (5xx, lost event) |
| WARN | unexpected but handled (retry, fallback, circuit open) |
| INFO | business milestones (user registered, application submitted) — the production default |
| DEBUG | developer detail (enabled per package locally) |
| TRACE | very verbose (SQL bind params) |

Configure levels in YAML: `logging.level.com.homefin: DEBUG`, `logging.level.org.hibernate.SQL: DEBUG`. At runtime: `POST /actuator/loggers/com.homefin` with `{"configuredLevel":"DEBUG"}` (if that endpoint is exposed).

**Rules:**
- log ids, not PII (`userId=…`, never emails, national IDs, tokens, passwords);
- one event = one line with key=value context;
- don't log *and* rethrow the same exception at every layer — log once where you handle it (the `GlobalExceptionHandler`);
- no `System.out.println`, no `e.printStackTrace()`.

## 11.2 Structured (JSON) logs

Spring Boot 3.4+ has built-in structured logging:

```yaml
logging.structured.format.console: ecs     # or: logstash, gelf
```

Enabled in the `docker` profile (`config-repo/application-docker.yml`) so log shippers (Fluent Bit, Filebeat, CloudWatch, Datadog) get fields instead of text. Locally we keep the human-readable pattern.

## 11.3 MDC & correlation

The **MDC** (Mapped Diagnostic Context) is a thread-local map whose values are added to every log line. With Micrometer Tracing on the classpath, Boot puts `traceId` and `spanId` in the MDC automatically, and the default console pattern shows them:

```
INFO [customer-service,6f1c0e3a9b...,2a7f...] c.h.c.customer.CustomerService : Customer profile created for userId=...
```

Our `GlobalExceptionHandler` copies the `traceId` into error responses. You can add your own keys (`MDC.put("customerId", ...)` in a filter — and always `MDC.remove` in `finally`).

## 11.4 Distributed tracing

Dependencies: `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`. Config:

```yaml
management:
  tracing.sampling.probability: 1.0                  # 100% locally; ~0.1 in production
  otlp.tracing.endpoint: http://localhost:4318/v1/traces
spring.kafka.template.observation-enabled: true     # propagate trace context through Kafka headers
spring.kafka.listener.observation-enabled: true
```

The gateway, incoming HTTP requests, RestClient, Feign and Kafka are instrumented out of the box (JDBC queries need the extra `net.ttddyy.observation:datasource-micrometer-spring-boot` dependency). Open **Jaeger** (<http://localhost:16686>), pick `api-gateway`, and follow *one* `POST /api/applications` through application-service → customer-service → WireMock → Kafka.

## 11.5 Metrics & health (Actuator)

| Endpoint | What |
|---|---|
| `/actuator/health` | UP/DOWN incl. DB, Kafka, disk, circuit breakers |
| `/actuator/health/liveness`, `/readiness` | Kubernetes probes |
| `/actuator/metrics` | list; e.g. `/actuator/metrics/http.server.requests` |
| `/actuator/prometheus` | Prometheus scrape format (HTTP latency histograms, JVM, Hikari pool, Kafka, Resilience4j) |
| `/actuator/info` | build/git info |

Custom business metric:

```java
private final MeterRegistry registry;
registry.counter("homefin.applications.submitted", "propertyType", type.name()).increment();
```

Or `@Observed(name = "kyc.verify")` on a method to get a timer + span at once. Visualise with Prometheus + Grafana; alert on error rate, latency (p95/p99), consumer lag, open circuit breakers, Hikari pool saturation.

## Sources
- Spring Boot logging (incl. structured logging): <https://docs.spring.io/spring-boot/reference/features/logging.html>
- Spring Boot observability/tracing: <https://docs.spring.io/spring-boot/reference/actuator/tracing.html>
- Actuator endpoints: <https://docs.spring.io/spring-boot/reference/actuator/endpoints.html>
- Micrometer docs: <https://docs.micrometer.io/micrometer/reference/>
- SLF4J manual: <https://www.slf4j.org/manual.html>
- OpenTelemetry: <https://opentelemetry.io/docs/>
- OWASP Logging cheat sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html>
