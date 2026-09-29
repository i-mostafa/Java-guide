# 4. Spring core concepts

## 4.1 Inversion of Control & Dependency Injection

In Express you wire things by hand: `const service = new CustomerService(new CustomerRepository(db))`.
In Spring, the **ApplicationContext** (the IoC container) creates objects — called **beans** — and injects their dependencies. NestJS copied this model, so if you know Nest providers, you know beans.

```java
@Service                              // "please manage this class as a bean"
@RequiredArgsConstructor              // Lombok: constructor with all final fields
public class CustomerService {
    private final CustomerRepository customers;   // injected by type
    private final KycClient kycClient;
}
```

Rules of thumb:
- **Constructor injection only** (final fields). Avoid `@Autowired` on fields — it hides dependencies and hurts testability. With a single constructor, `@Autowired` isn't even needed.
- Beans are **singletons** by default: one instance shared by all threads → keep them **stateless**.
- Too many constructor params = the class does too much; split it.

### Stereotype annotations (all are `@Component`)

| Annotation | Layer | Extra behaviour |
|---|---|---|
| `@RestController` | web | `@Controller` + `@ResponseBody` (returns JSON) |
| `@Service` | business logic | none (semantic) |
| `@Repository` | persistence | exception translation to `DataAccessException` (Spring Data repos get it automatically) |
| `@Component` | anything else | — |
| `@Configuration` | config | contains `@Bean` factory methods |

**Component scanning** starts at the package of the `@SpringBootApplication` class and goes down. A class outside that tree isn't found — a common "bean not found" cause.

### `@Configuration` + `@Bean` for things you don't own

```java
@Configuration
public class KycClientConfig {
    @Bean
    RestClient kycRestClient(RestClient.Builder builder, KycProperties props) {   // params are injected
        return builder.baseUrl(props.baseUrl()).build();
    }
}
```

When two beans of the same type exist, disambiguate with `@Qualifier("name")` or `@Primary`, or inject a `List<Type>` to get all of them.

## 4.2 Configuration: `application.yml`, profiles, properties

Spring merges configuration from many **property sources** (highest wins): command-line args → environment variables → config-server → `application-{profile}.yml` → `application.yml`.

- Env vars map by relaxed binding: `SPRING_DATASOURCE_URL` → `spring.datasource.url`, `APP_KYC_BASE_URL` → `app.kyc.base-url`.
- Placeholders with defaults: `${KYC_API_KEY:local-dev-key}` (≈ `process.env.KYC_API_KEY ?? 'local-dev-key'`).
- **Profiles** activate extra config/beans: `SPRING_PROFILES_ACTIVE=docker` loads `application-docker.yml`; `@Profile("!prod")` on `AdminBootstrap` keeps it out of production.

**Type-safe config** — prefer `@ConfigurationProperties` records over scattered `@Value`:

```java
@Validated
@ConfigurationProperties(prefix = "app.kyc")
public record KycProperties(@NotBlank String baseUrl, @NotBlank String apiKey,
                            @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {}
```
```yaml
app:
  kyc:
    base-url: http://localhost:9090
    connect-timeout: 2s          # Duration parsing for free: 500ms, 2s, 5m
```

Registered via `@ConfigurationPropertiesScan` on the application class. `@Validated` = the app refuses to start with bad config (fail fast). The `spring-boot-configuration-processor` gives you IDE auto-completion for your own properties.

**Secrets** never go in `application.yml` in git: use env vars, Kubernetes secrets, Vault, AWS Secrets Manager (Spring Cloud AWS / Spring Cloud Vault).

## 4.3 Auto-configuration — the "magic" explained

`@SpringBootApplication` enables auto-configuration: Boot inspects the classpath and your properties and creates beans **you didn't declare**, guarded by conditions:

- `spring-boot-starter-data-jpa` on classpath + `spring.datasource.url` set → `DataSource`, `EntityManagerFactory`, `TransactionManager`.
- `spring-kafka` on classpath → `KafkaTemplate`, listener container factory (and it picks up *your* `DefaultErrorHandler` bean).
- You define your own bean of the same type → Boot's backs off (`@ConditionalOnMissingBean`). E.g. auth-service defines its own `JwtDecoder`.

Our `common-lib` ships a **custom auto-configuration** (`CommonWebAutoConfiguration`, registered in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`) — that is exactly how every starter works.

Debugging the magic:
- start with `--debug` (or `debug: true`) → **condition evaluation report** (what was configured and why).
- `/actuator/beans`, `/actuator/conditions`, `/actuator/configprops`, `/actuator/env` (expose them only locally).

## 4.4 Proxies & AOP — the #1 source of "why doesn't my annotation work?"

`@Transactional`, `@PreAuthorize`, `@Retry`, `@CircuitBreaker`, `@Cacheable`, `@Async` work by wrapping your bean in a **proxy**. The proxy intercepts calls *coming from other beans*.

Consequences:
1. **Self-invocation bypasses the proxy:** `this.save()` inside the same class → no transaction, no retry. Fix: move the method to another bean (why `KycClient` is separate from `CustomerService`), or use `TransactionTemplate` (see `FinanceApplicationService.submit`).
2. Annotated methods must be **public** (and the class not `final`).
3. Order matters when stacking aspects (Resilience4j: `Retry(CircuitBreaker(...))`).

## 4.5 Bean lifecycle & startup hooks

| Need | Use |
|---|---|
| run code once the app is ready | `ApplicationRunner` / `CommandLineRunner` bean (`AdminBootstrap`) or `@EventListener(ApplicationReadyEvent.class)` |
| init after injection | `@PostConstruct` method |
| cleanup | `@PreDestroy` (and `server.shutdown: graceful`) |

## 4.6 Application events (in-process pub/sub)

```java
events.publishEvent(new UserRegisteredDomainEvent(...));        // AuthService

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)  // UserEventsPublisher
public void on(UserRegisteredDomainEvent e) { kafkaTemplate.send(...); }
```

Like Node's `EventEmitter`, but can be bound to the transaction outcome. Great for decoupling "do the thing" from "tell others about it".

## Sources
- IoC container: <https://docs.spring.io/spring-framework/reference/core/beans.html>
- Externalized configuration: <https://docs.spring.io/spring-boot/reference/features/external-config.html>
- Profiles: <https://docs.spring.io/spring-boot/reference/features/profiles.html>
- Auto-configuration & creating your own: <https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html>
- AOP proxies: <https://docs.spring.io/spring-framework/reference/core/aop/proxying.html>
- Application events: <https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events>
