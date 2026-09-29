# 2. Java for TypeScript developers

Java and TypeScript look similar (C-style syntax, classes, generics), but the runtime model is different:

| | TypeScript / Node | Java |
|---|---|---|
| Types | structural, erased at runtime, optional (`any`) | **nominal**, checked at compile time *and* partly at runtime; no `any` (closest: `Object`) |
| Compile | `tsc` → JS, run by V8 | `javac` → bytecode (`.class`) in a `.jar`, run by the **JVM** (JIT-compiled) |
| Concurrency | one event loop, async I/O, `Promise` | **many threads**; blocking I/O is normal; Java 21 **virtual threads** make blocking cheap |
| Modules | files + `import` from paths | **packages** (folders) + `import` of fully-qualified class names |
| Null | `undefined` + `null`, `?.`, `??` | only `null`; `Optional<T>` for return values; NPE if you're careless |
| Numbers | one `number` (double) | `int`, `long`, `double`, … + `BigDecimal` for money |

## 2.1 Files, packages, classes

One public top-level type per file; the file name = class name; the folder = the package.

```java
// src/main/java/com/homefin/customer/customer/CustomerService.java
package com.homefin.customer.customer;          // must match the folder

import com.homefin.customer.kyc.KycClient;      // import a class by its fully-qualified name
import java.util.UUID;

public class CustomerService { ... }
```

Access modifiers: `public` (everyone), `protected` (package + subclasses), *(none)* = package-private (same package only — a common Spring default for `@Bean` methods and test classes), `private` (the class only).

## 2.2 Variables and types

```ts
// TS
const name: string = "Jane";
let count = 0;
const ids: string[] = [];
const byId: Record<string, User> = {};
```

```java
// Java
final String name = "Jane";               // final = const (reference can't be reassigned)
var count = 0;                            // `var` = local type inference (Java 10+), still static typing
List<String> ids = new ArrayList<>();     // interfaces on the left, implementation on the right
Map<String, User> byId = new HashMap<>();
```

Primitives (`int`, `long`, `boolean`, `double`) vs boxed types (`Integer`, `Long`, ...). Collections only hold objects, so `List<Integer>`, and a boxed `Integer` can be `null` — unboxing a `null` throws `NullPointerException`. Use `Integer`/`Long` in DTOs where "missing" is meaningful (validation with `@NotNull`), primitives elsewhere.

**Strings**: compare with `.equals()`, never `==` (which compares references). Text blocks for multi-line:

```java
String json = """
        {"email": "a@b.com"}
        """;
String msg = "Customer %s not found".formatted(id);   // like template literals
```

**Money:** always `BigDecimal`, never `double` (`0.1 + 0.2 != 0.3`). Compare with `compareTo`, not `equals` (`equals` also compares scale: `2.0 != 2.00`). See `InstallmentCalculator`.

**Dates:** `java.time` — `Instant` (a UTC timestamp, like `Date.now()`), `LocalDate` (a calendar date, e.g. date of birth), `Duration`, `ZonedDateTime`. Never `java.util.Date`. Inject a `Clock` for testability (`ClockConfig`, `TokenService`).

## 2.3 Classes, records, interfaces, enums

**Record** = immutable data carrier (auto constructor, accessors, `equals`/`hashCode`/`toString`). Your go-to for DTOs and events — the equivalent of a TS `type`/`interface` for data:

```java
public record CustomerSummary(UUID id, String firstName, String kycStatus) {
    public boolean isKycVerified() { return "VERIFIED".equals(kycStatus); }  // methods allowed
}
var c = new CustomerSummary(id, "Jane", "VERIFIED");
c.firstName();       // accessor has no "get" prefix
```

**Class** = state + behaviour; used for entities and services:

```java
public class Customer {
    private String email;                      // fields are private
    public String getEmail() { return email; } // JavaBean getters (Lombok @Getter generates these)
}
```

**Interface** = contract (can have `default` methods). Spring Data repositories and Feign clients are *just interfaces*; the framework generates the implementation.

**Enum** = a real class with fields/methods — far more powerful than TS enums (see the state machine in `ApplicationStatus.canTransitionTo`).

**Inheritance:** `extends` one class, `implements` many interfaces. Prefer composition; the repo only uses inheritance for `BaseEntity`.

**Sealed types + pattern matching** (Java 17–21) give you TS-like discriminated unions:

```java
sealed interface PaymentResult permits Approved, Declined {}
record Approved(String ref) implements PaymentResult {}
record Declined(String reason) implements PaymentResult {}

String describe(PaymentResult r) {
    return switch (r) {                       // exhaustive: compiler errors if a case is missing
        case Approved a -> "OK " + a.ref();
        case Declined d -> "NO " + d.reason();
    };
}
if (t instanceof ApiException api) { throw api; }   // pattern-matching instanceof (see KycClient)
```

## 2.4 Generics

Same idea as TS, but erased at runtime (you can't do `new T()` or `T.class`). Wildcards express variance:

```java
List<? extends Number>   // read-only-ish "some subtype of Number"  (TS: readonly Number[])
List<? super Integer>    // can add Integers
public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) { ... }  // see common-lib
```

## 2.5 Null handling

Java has no `?.` or `??`. Tools:

- `Optional<T>` for **return values** that may be absent (Spring Data: `findByUserId` returns `Optional<Customer>`):
  ```java
  customers.findByUserId(userId).orElseThrow(() -> new NotFoundException("Customer", userId));
  optional.map(Customer::getEmail).orElse("n/a");
  ```
  Don't use `Optional` for fields or parameters.
- `Objects.requireNonNull(x, "x")` at boundaries, `Objects.equals(a, b)` for null-safe equality.
- Validation (`@NotNull`) at the API boundary so the inside of your code can assume non-null.

## 2.6 Collections and streams (≈ array methods)

| TS | Java |
|---|---|
| `arr.map(f)` | `list.stream().map(f).toList()` |
| `arr.filter(p)` | `.filter(p)` |
| `arr.find(p)` | `.filter(p).findFirst()` → `Optional` |
| `arr.some(p)` / `every` | `.anyMatch(p)` / `.allMatch(p)` |
| `arr.reduce((a,b)=>a+b, 0)` | `.reduce(0, Integer::sum)` or `.mapToInt(x -> x).sum()` |
| `groupBy` | `.collect(Collectors.groupingBy(Customer::getKycStatus))` |
| `[...new Set(arr)]` | `.distinct()` / `new HashSet<>(list)` |
| `Object.entries(o)` | `map.entrySet()` |
| `[1,2,3]` literal | `List.of(1, 2, 3)` (immutable!) |

Lambdas: `x -> x * 2`, `(a, b) -> a + b`; method references: `mapper::toResponse`, `String::trim`. See `GlobalExceptionHandler.handleMethodArgumentNotValid` for a real stream.

`List.of`, `Map.of`, `.toList()` return **immutable** collections — calling `.add()` throws. Use `new ArrayList<>(...)` if you need to mutate.

## 2.7 Exceptions

- **Checked** exceptions (`IOException`, `Exception` subclasses) must be declared (`throws`) or caught — the compiler enforces it.
- **Unchecked** (`RuntimeException` subclasses) don't. **Spring code uses unchecked exceptions almost exclusively** (see `ApiException`), and Spring translates DB errors into unchecked `DataAccessException`s.
- Never swallow exceptions (`catch (Exception e) {}`); either handle, wrap with context (`new X("msg", e)` keeps the cause), or let them propagate to the `@RestControllerAdvice`.
- `try-with-resources` closes things automatically (like `using`): `try (var in = Files.newInputStream(p)) { ... }`.
- In Spring, a `RuntimeException` thrown from a `@Transactional` method **rolls back** the transaction; checked ones don't by default.

## 2.8 equals / hashCode / toString

Objects are compared by reference unless you override `equals` and `hashCode` (records do it for you). Rules: override both together; `HashMap`/`HashSet` depend on them. JPA entities need special care (see `BaseEntity`). Override `toString` to hide secrets — records print *every* field (see `RegisterRequest.toString`).

## 2.9 Concurrency (the biggest mindset shift)

Node: one thread, never block, everything `async`. Spring MVC: **one thread per request** — plain blocking code (`repository.findById(...)`, `restClient.get()...`) is normal and simple.

- **Virtual threads** (Java 21): cheap threads so blocking I/O scales like async. Enabled here with `spring.threads.virtual.enabled: true`. You write normal sequential code.
- `CompletableFuture<T>` ≈ `Promise<T>` (`thenApply` ≈ `then`, `thenCompose` ≈ chaining promises, `CompletableFuture.allOf` ≈ `Promise.all`). `KafkaTemplate.send(...)` returns one (`whenComplete` in `UserEventsPublisher`).
- Shared mutable state across threads needs care: prefer immutable objects, local variables, `ConcurrentHashMap`, `AtomicLong`. Spring singleton beans are shared by all request threads — **never store request data in a bean field**.
- `ThreadLocal`s carry per-request context (Spring Security's `SecurityContextHolder`, logging `MDC`). They don't automatically follow you into another thread/executor.
- WebFlux/Reactor (`Mono`, `Flux`) is Spring's non-blocking stack — used by the gateway. Don't mix blocking calls into reactive code.

## 2.10 Lombok (used in this repo)

Generates boilerplate at compile time:

| Annotation | Generates | Use |
|---|---|---|
| `@Getter` / `@Setter` | accessors | entities (getters only, prefer domain methods to setters) |
| `@RequiredArgsConstructor` | constructor for `final` fields | **services — constructor injection** |
| `@Slf4j` | `private static final Logger log` | everywhere you log |
| `@Builder` | fluent builder | objects with many fields (`FinanceApplication.builder()`) |
| `@NoArgsConstructor(access = PROTECTED)` | no-args ctor | JPA entities |
| ~~`@Data`~~ | getters, setters, equals, hashCode, toString | **avoid on entities** (breaks Hibernate, leaks lazy fields) |

Many teams prefer records over Lombok for DTOs; follow your project's conventions.

## 2.11 Practice

Open `jshell` and try:

```java
var list = List.of("b", "a", "c");
list.stream().sorted().map(String::toUpperCase).toList();
new java.math.BigDecimal("0.1").add(new java.math.BigDecimal("0.2"));
java.time.LocalDate.now().minusYears(18);
```

## Sources
- dev.java — official learning path: <https://dev.java/learn/>
- Java 21 language features overview: <https://docs.oracle.com/en/java/javase/21/language/>
- Records (JEP 395): <https://openjdk.org/jeps/395> · Pattern matching for switch (JEP 441): <https://openjdk.org/jeps/441> · Virtual threads (JEP 444): <https://openjdk.org/jeps/444>
- *Effective Java*, 3rd ed., Joshua Bloch — the classic best-practices book
- Lombok features: <https://projectlombok.org/features/>
