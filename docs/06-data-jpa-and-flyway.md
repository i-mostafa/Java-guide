# 6. Data: JPA, transactions & Flyway

## 6.1 The stack

**JPA** (Jakarta Persistence) is the spec, **Hibernate** the implementation (ORM), **Spring Data JPA** adds repository interfaces on top, **HikariCP** is the connection pool, **Flyway** owns the schema. Rough TypeORM equivalent, but with a *persistence context* (unit of work) you must understand.

## 6.2 Entities

```java
@Entity
@Table(name = "customers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // JPA needs a no-args ctor; hide it from app code
public class Customer extends BaseEntity {           // id (UUID), createdAt, updatedAt, @Version

    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)                       // ALWAYS STRING, never ORDINAL
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus = KycStatus.PENDING;

    public void markKycResult(boolean approved, String ref, Instant at) { ... }   // behaviour, not setters
}
```

Best practices applied in [`BaseEntity`](../common-lib/src/main/java/com/homefin/common/persistence/BaseEntity.java):
- UUID ids (safe to expose, generated app-side),
- `@CreatedDate`/`@LastModifiedDate` via `@EnableJpaAuditing`,
- `@Version` for **optimistic locking**,
- Hibernate-safe `equals`/`hashCode`; **no Lombok `@Data`** on entities.

**Relationships** (`@ManyToOne`, `@OneToMany`) — this repo avoids them (each service has few tables), but in a monolith you'll see many. Rules: make `@ManyToOne(fetch = LAZY)` (default is EAGER!), keep `@OneToMany` lazy (default), own the relationship on the `@ManyToOne` side, and never serialize entities with lazy relations to JSON.

Across **services** you never use relationships — you store the other service's id (`customerUserId`) and ask the other service (API/events).

## 6.3 Repositories

```java
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByUserId(UUID userId);                          // derived query
    Page<Customer> findAllByKycStatus(KycStatus status, Pageable pageable); // + paging/sorting
    @Query("select count(c) from Customer c where c.kycStatus = :status")   // JPQL: entities, not tables
    long countByStatus(KycStatus status);
}
```

`JpaRepository` gives you `save`, `findById`, `findAll(Pageable)`, `delete`, `count`... Other tools: `@Query(nativeQuery = true)` for raw SQL, **projections** (interface or record return types to select only some columns), `JpaSpecificationExecutor` for dynamic filters, `@Modifying @Query("update ...")` for bulk updates, `@EntityGraph` to fetch relations eagerly per query.

## 6.4 Transactions

```java
@Transactional                   // begin; commit on success; rollback on RuntimeException
public Customer updateProfile(UUID userId, UpdateProfileRequest r) {
    Customer c = getByUserId(userId);   // loaded into the persistence context ("managed")
    c.updateProfile(...);               // just change it...
    return c;                           // ...Hibernate issues the UPDATE at commit ("dirty checking")
}
```

- Put `@Transactional` on **service** methods (not controllers, not repositories).
- `@Transactional(readOnly = true)` for queries (optimisations, and intent).
- Checked exceptions don't roll back unless `rollbackFor = ...`.
- Self-invocation skips the proxy (chapter 4.4). Use `TransactionTemplate` for programmatic control.
- **Never call slow remote services inside a transaction** — you hold a DB connection and locks while waiting. See `CustomerService.verifyKyc` and `FinanceApplicationService.submit`: remote calls first, then a short transaction.
- Catching a DB exception *inside* a transaction still marks it rollback-only → `UnexpectedRollbackException` at commit (see the comment in `CustomerService.createFromRegistration`).
- `spring.jpa.open-in-view: false` — keeps DB access out of the web layer; lazy loading outside a transaction then fails loudly (good) instead of silently running queries during JSON rendering.

## 6.5 Performance pitfalls

- **N+1 queries**: loading 50 applications and touching `app.getCustomer()` on each = 51 queries. Fix with `join fetch` in JPQL, `@EntityGraph`, or DTO projections. Turn on `logging.level.org.hibernate.SQL=DEBUG` (and `org.hibernate.orm.jdbc.bind=TRACE` to see parameters) while developing.
- Pagination + `join fetch` of collections = in-memory paging (Hibernate warns) — page ids first.
- Batch inserts: `spring.jpa.properties.hibernate.jdbc.batch_size=50` (not effective with `IDENTITY` ids — another reason for UUID/sequence ids).
- Index the columns you filter/sort by (see the migrations).
- Tune the Hikari pool size to DB capacity (`spring.datasource.hikari.maximum-pool-size`) — more is not better.

## 6.6 Flyway migrations

`src/main/resources/db/migration/V1__create_customers.sql`, `V2__...`: applied in order on startup, recorded in `flyway_schema_history`, checksummed.

- **Never edit an applied migration** — add a new one (`V2__add_status_check.sql` in application-service).
- `spring.jpa.hibernate.ddl-auto: validate` — Hibernate only checks the entities match the schema. Never use `update`/`create` beyond throwaway prototypes.
- Zero-downtime changes are *expand → migrate → contract*: add the new nullable column, deploy code that writes both, backfill, switch reads, drop the old column later.
- Liquibase is the common alternative (XML/YAML changelogs); the principles are identical.

## 6.7 Database per service

Each service owns its database (`authdb`, `customerdb`, `applicationdb`) — no shared tables, no cross-service joins. Data needed elsewhere is obtained through APIs or **replicated via events** (customer-service builds its customer rows from `UserRegisteredEvent`).

## Sources
- Spring Data JPA reference: <https://docs.spring.io/spring-data/jpa/reference/>
- Transaction management: <https://docs.spring.io/spring-framework/reference/data-access/transaction.html>
- Hibernate ORM user guide: <https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html>
- Flyway docs: <https://documentation.red-gate.com/fd>
- Vlad Mihalcea — equals/hashCode for entities: <https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/>
- Vlad Mihalcea — N+1 query problem: <https://vladmihalcea.com/n-plus-1-query-problem/>
- Open Session in View anti-pattern: <https://vladmihalcea.com/the-open-session-in-view-anti-pattern/>
