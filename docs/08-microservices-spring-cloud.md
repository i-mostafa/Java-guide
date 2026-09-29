# 8. Microservices with Spring Cloud

## 8.1 The pieces in this repo

| Concern | Component | Where |
|---|---|---|
| Single entry point, routing, edge auth | **Spring Cloud Gateway** (reactive) | `api-gateway` |
| Find instances by name | **Eureka** (Spring Cloud Netflix) | `discovery-server` |
| Central, per-environment configuration | **Spring Cloud Config** | `config-server` |
| Declarative service-to-service HTTP | **OpenFeign** + Spring Cloud LoadBalancer | `application-service/customer/CustomerClient` |
| Resilience | **Resilience4j** | `CustomerGateway`, `ValuationGateway`, `KycClient` |
| Async integration | **Kafka** | chapter 9 |
| Tracing across services | Micrometer Tracing + OpenTelemetry → Jaeger | chapter 11 |

> On **Kubernetes** you often *drop* Eureka (use K8s Services/DNS) and Config Server (use ConfigMaps/Secrets, or Spring Cloud Kubernetes), and may replace the gateway with an ingress/API gateway product. The application code (Feign, Resilience4j, Kafka) stays the same. Check what your project uses.

## 8.2 API Gateway

Routes (`api-gateway/application.yml`, Spring Cloud Gateway 4.3+ property names):

```yaml
spring.cloud.gateway.server.webflux.routes:
  - id: customer-service
    uri: lb://customer-service          # "lb://" = resolve via discovery + client-side load balancing
    predicates: [ Path=/api/customers/** ]
```

- Predicates match (path, method, header, host...), filters modify (`AddRequestHeader`, `RewritePath`, `Retry`, `CircuitBreaker`, `RequestRateLimiter` with Redis...).
- A `GlobalFilter` runs for every route (`RequestLoggingFilter`).
- The gateway is **reactive** (Netty + WebFlux): never call blocking code in it. (There is also a servlet-based "Gateway Server MVC" variant.)
- Security at the edge doesn't replace security in services (zero trust).

## 8.3 Service discovery

Services register on startup (`spring.application.name` = service id) and send heartbeats. Callers look up instances by name: `lb://customer-service` in the gateway, `@FeignClient(name = "customer-service")` in code. Dashboard: <http://localhost:8761>.

## 8.4 Central configuration

Clients: `spring.config.import: optional:configserver:http://localhost:8888`. The server merges, for app `customer-service` and profile `docker`:

`customer-service-docker.yml` > `application-docker.yml` > `customer-service.yml` > `application.yml` (all in `config-server/src/main/resources/config-repo/`).

Try <http://localhost:8888/customer-service/docker>. In production the backend is a **git repo** (auditable, reviewable config changes) or Vault; secrets can be encrypted (`{cipher}...`). Refresh without restart via `@RefreshScope` + `/actuator/refresh` (or Spring Cloud Bus).

## 8.5 Synchronous calls: OpenFeign

```java
@FeignClient(name = "customer-service", url = "${app.clients.customer-service.url:}", path = "/api/customers")
public interface CustomerClient {
    @GetMapping("/me")
    CustomerSummary currentCustomer();
}
```

- Enable with `@EnableFeignClients`.
- Timeouts: `spring.cloud.openfeign.client.config.default.connect-timeout/read-timeout` — **always set them**.
- A `RequestInterceptor` relays the user's JWT (`FeignConfig`).
- Wrap the client in a small "gateway" bean that translates errors (`FeignException.NotFound` → business error) and adds a circuit breaker (`CustomerGateway`).
- Define your **own** DTO for the response (`CustomerSummary`) with just the fields you need — don't import the other service's classes.
- Spring's own **HTTP Interfaces** (`@HttpExchange`, chapter 10) are a Feign alternative; the Spring Cloud OpenFeign docs describe the project as feature-complete and point to HTTP Interfaces as the alternative for new code. Both are common in existing projects.

## 8.6 Sync vs async — choosing

| Use sync (HTTP) when | Use async (Kafka) when |
|---|---|
| you need an answer now (is the customer verified?) | you're announcing a fact (user registered, status changed) |
| the caller can't proceed without it | consumers can react later; eventual consistency is OK |
| | you want loose coupling / fan-out to many consumers |

Every sync call adds latency and a failure mode → timeouts + retries (idempotent calls only) + circuit breakers + fallbacks.

## 8.7 Patterns you'll hear about

- **Database per service** — chapter 6.7.
- **Saga** — a multi-service business transaction as a sequence of local transactions + compensations (choreography via events, or orchestration via e.g. **Temporal**/Camunda).
- **Transactional Outbox** — chapter 9.7.
- **API composition / BFF** — gateway or a dedicated backend aggregates several services for one screen.
- **Strangler fig** — migrate a monolith piece by piece behind the gateway.
- **Idempotency everywhere** — networks retry; make handlers safe to run twice.

## Sources
- Spring Cloud overview & release train/Boot compatibility: <https://spring.io/projects/spring-cloud>
- Spring Cloud Gateway: <https://docs.spring.io/spring-cloud-gateway/reference/>
- Spring Cloud Config: <https://docs.spring.io/spring-cloud-config/reference/>
- Spring Cloud Netflix (Eureka): <https://docs.spring.io/spring-cloud-netflix/reference/>
- Spring Cloud OpenFeign: <https://docs.spring.io/spring-cloud-openfeign/reference/>
- microservices.io patterns (Chris Richardson): <https://microservices.io/patterns/>
- *Building Microservices*, 2nd ed., Sam Newman
