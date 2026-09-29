# 12. Testing

## 12.1 Tools (all included by `spring-boot-starter-test`)

| Jest world | Java world |
|---|---|
| jest runner, `describe/it` | **JUnit 5** (`@Test`, `@ParameterizedTest`, `@Nested`, `@BeforeEach`) |
| `expect(x).toBe(y)` | **AssertJ** `assertThat(x).isEqualTo(y)` (fluent, great messages) |
| `jest.fn()`, `jest.mock()` | **Mockito** (`@Mock`, `given(...).willReturn(...)`, `then(mock).should()...`) |
| supertest | **MockMvc** |
| `waitFor` | **Awaitility** |
| docker-compose for tests | **Testcontainers** |

Naming: `*Test` = unit/slice tests (surefire, `mvn test`); `*IT` = integration tests (failsafe, `mvn verify`).

## 12.2 The pyramid in this repo

| Level | Example | Spring context? | Speed |
|---|---|---|---|
| Pure unit | `InstallmentCalculatorTest`, `AdultValidatorTest`, `TokenServiceTest` | no | ms |
| Unit with mocks | `CustomerServiceTest`, `FinanceApplicationServiceTest` (`@ExtendWith(MockitoExtension.class)`) | no | ms |
| Validation | `CreateApplicationRequestValidationTest` (plain `Validator`) | no | ms |
| Web slice | `CustomerControllerTest` (`@WebMvcTest`) | MVC layer only | ~1 s |
| Full integration | `AuthFlowIT`, `UserRegisteredListenerIT`, `FinanceApplicationFlowIT` (`@SpringBootTest` + Testcontainers) | full | seconds |

Most tests should be at the bottom. Business logic in plain classes = easy unit tests.

## 12.3 Unit test with Mockito

```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock CustomerRepository customers;
    @Mock KycClient kycClient;
    @InjectMocks CustomerService service;

    @Test
    void kycRequiresCompleteProfile() {
        given(customers.findByUserId(userId)).willReturn(Optional.of(Customer.register(userId, "a@b.com", "A", "B")));

        assertThatThrownBy(() -> service.verifyKyc(userId))
                .isInstanceOf(BusinessRuleException.class);
        then(kycClient).should(never()).verify(any(), anyString());
    }
}
```

## 12.4 Slice tests

`@WebMvcTest(CustomerController.class)` starts only MVC infrastructure: controller, JSON, validation, `@ControllerAdvice`s in your packages, filters. Everything else is mocked with `@MockitoBean` (Spring Boot 3.4+; replaces the deprecated `@MockBean`). Import what the slice doesn't pick up (`@Import(SecurityConfig.class)`, `@ImportAutoConfiguration(CommonWebAutoConfiguration.class)`).

Other slices: `@DataJpaTest` (repositories + real DB via Testcontainers), `@JsonTest`, `@RestClientTest`, `@WebFluxTest`.

## 12.5 Integration tests with Testcontainers

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {
    @Bean @ServiceConnection PostgreSQLContainer<?> postgres() { return new PostgreSQLContainer<>("postgres:17-alpine"); }
    @Bean @ServiceConnection KafkaContainer kafka() { return new KafkaContainer("apache/kafka-native:3.8.0"); }
}

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Import(TestcontainersConfig.class)
class AuthFlowIT { ... }
```

`@ServiceConnection` wires `spring.datasource.*` / `spring.kafka.*` to the containers — no manual properties. Flyway runs the real migrations, so schema bugs surface in tests. Containers declared as beans are reused across test classes that share the same context configuration (Spring caches contexts — keep test configuration consistent so contexts are reused).

**Bonus:** run the app locally against containers with no docker-compose: create `TestCustomerServiceApplication` with `SpringApplication.from(CustomerServiceApplication::main).with(TestcontainersConfig.class).run(args)`.

## 12.6 Good practices

- Test behaviour, not implementation; one reason to fail per test; descriptive names (`rejectsWhenKycNotVerified`).
- Arrange / Act / Assert (given / when / then) layout.
- Don't `Thread.sleep` — use Awaitility for async.
- Freeze time with an injected `Clock`.
- Money: `isEqualByComparingTo("5845.90")`.
- Keep `@SpringBootTest` tests few and meaningful; they're slow.
- Coverage (JaCoCo) is a guide, not a goal. Mutation testing (PIT) finds weak assertions.
- Architecture rules (e.g. "controllers don't use repositories") can be enforced with **ArchUnit**.

## Sources
- Spring Boot testing: <https://docs.spring.io/spring-boot/reference/testing/index.html>
- Testcontainers & `@ServiceConnection`: <https://docs.spring.io/spring-boot/reference/testing/testcontainers.html>
- Testcontainers for Java: <https://java.testcontainers.org>
- Spring Security testing: <https://docs.spring.io/spring-security/reference/servlet/test/index.html>
- JUnit 5 user guide: <https://junit.org/junit5/docs/current/user-guide/>
- AssertJ: <https://assertj.github.io/doc/> · Mockito: <https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html>
- ArchUnit: <https://www.archunit.org>
