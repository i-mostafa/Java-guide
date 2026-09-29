# 10. Calling 3rd-party APIs & resilience

## 10.1 Which HTTP client?

| Client | Style | Use for |
|---|---|---|
| **`RestClient`** (Spring 6.1+) | fluent, synchronous | default choice for new code — `KycClient` |
| **HTTP Interface** (`@HttpExchange`) | declarative interface, backed by RestClient/WebClient | typed clients for external APIs — `ValuationClient` |
| **OpenFeign** | declarative, integrates with discovery/load balancing | service-to-service in Spring Cloud setups — `CustomerClient` |
| `WebClient` | reactive (`Mono`/`Flux`) | reactive apps (gateway), streaming |
| `RestTemplate` | legacy, synchronous | you'll see it in older code; prefer RestClient for new code |

Always build clients from the Boot-provided `RestClient.Builder` (prototype bean): it's pre-wired with Jackson and **observability**, so every outbound call gets a tracing span and propagates the `traceparent` header.

## 10.2 RestClient example (KYC provider)

```java
@Bean
RestClient kycRestClient(RestClient.Builder builder, KycProperties props) {
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(props.connectTimeout()).build();
    JdkClientHttpRequestFactory rf = new JdkClientHttpRequestFactory(httpClient);
    rf.setReadTimeout(props.readTimeout());                 // ALWAYS set timeouts
    return builder.baseUrl(props.baseUrl()).requestFactory(rf)
                  .defaultHeader("X-Api-Key", props.apiKey()).build();
}

return kycRestClient.post()
        .uri("/kyc/v1/verifications")
        .header("Idempotency-Key", idempotencyKey)           // safe retries of a POST
        .body(request)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> { throw new BusinessRuleException(...); })
        .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> { throw new KycProviderException(...); })
        .body(KycVerificationResponse.class);
```

## 10.3 HTTP Interface example (valuation provider)

```java
@HttpExchange(url = "/valuation/v1", accept = "application/json", contentType = "application/json")
public interface ValuationClient {
    @PostExchange("/estimates")
    ValuationResponse estimate(@RequestBody ValuationRequest request);
}

// wiring (ValuationClientConfig)
HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build().createClient(ValuationClient.class);
```

## 10.4 Integration best practices

1. **Anti-corruption layer:** the provider's request/response types (`KycVerificationRequest/Response`) live in the integration package and are translated to your domain; provider concepts don't leak into your model.
2. **Tolerant reader:** `@JsonIgnoreProperties(ignoreUnknown = true)` so new provider fields don't break you (Boot's ObjectMapper already disables `FAIL_ON_UNKNOWN_PROPERTIES`).
3. **Timeouts** on connect *and* read, sized to your own SLA.
4. **Classify errors:** 4xx = our bug or bad input (don't retry) → business error; 5xx/timeouts = transient (retry) → 503 if it persists.
5. **Idempotency keys** for retried POSTs.
6. **Secrets** (API keys) from env/secret manager; never log them or full request bodies with PII.
7. **Never inside a DB transaction** (chapter 6.4).
8. Keep the base URL, timeouts and keys in a validated `@ConfigurationProperties` record.

## 10.5 Resilience4j

Declared with annotations (via AOP — the method must be called from another bean), configured in YAML:

```java
@Retry(name = "kyc", fallbackMethod = "fallback")   // outermost
@CircuitBreaker(name = "kyc")
public KycVerificationResponse verify(KycVerificationRequest request, String idempotencyKey) { ... }

private KycVerificationResponse fallback(KycVerificationRequest request, String key, Throwable t) { ... }  // same params + Throwable
```

```yaml
resilience4j:
  retry.instances.kyc:
    max-attempts: 3
    wait-duration: 500ms
    enable-exponential-backoff: true
    retry-exceptions: [com.homefin.customer.kyc.KycProviderException, org.springframework.web.client.ResourceAccessException]
  circuitbreaker.instances.kyc:
    sliding-window-size: 10
    minimum-number-of-calls: 5
    failure-rate-threshold: 50           # open when ≥50% of the last 10 calls failed
    wait-duration-in-open-state: 30s     # then half-open and probe
    ignore-exceptions: [com.homefin.common.error.BusinessRuleException]   # business errors aren't outages
```

| Pattern | Protects against |
|---|---|
| Timeout | hanging forever |
| Retry (+ backoff + jitter) | transient blips — only for idempotent operations |
| Circuit breaker | hammering a dead dependency; fail fast, give it time to recover |
| Bulkhead | one slow dependency exhausting all threads |
| Rate limiter | exceeding a provider's quota |
| Fallback | returning a controlled error / cached / default value |

Observe it: `/actuator/circuitbreakers`, `/actuator/health` (breaker state), Prometheus metrics `resilience4j_*`.
Try it: set a customer's `nationalId` to start with `999` → WireMock returns 503 → watch the retries in the logs and the breaker open after repeated calls.

## 10.6 Testing integrations

- **WireMock** stubs real HTTP (here as a docker container with JSON mappings in `infra/wiremock/mappings`; in tests via `org.wiremock:wiremock-standalone` or `wiremock-spring-boot`).
- `MockRestServiceServer` (with `@RestClientTest`) for fast client unit tests.
- Mock the gateway bean (`@MockitoBean ValuationGateway`) in service-level integration tests (`FinanceApplicationFlowIT`).
- Contract testing (Spring Cloud Contract, Pact) for service-to-service APIs you own.

## Sources
- REST clients in Spring (RestClient, HTTP Interfaces): <https://docs.spring.io/spring-framework/reference/integration/rest-clients.html>
- Spring Boot REST client auto-configuration: <https://docs.spring.io/spring-boot/reference/io/rest-client.html>
- Resilience4j docs: <https://resilience4j.readme.io/docs/getting-started-3>
- WireMock docs: <https://wiremock.org/docs/>
- Timeouts, retries and backoff with jitter (AWS Builders' Library): <https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/>
