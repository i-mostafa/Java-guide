package com.homefin.customer.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// Optional<T> = a box that either holds a value or is empty; the explicit alternative to returning null
// (like T | undefined, but the caller must unwrap it: orElseThrow(), map(), isPresent()...).
import java.util.Optional;
import java.util.UUID;

/**
 * Data-access layer for {@link Customer}: like a TypeORM {@code Repository<Customer>} or a Prisma model client.
 *
 * <p>This is only an {@code interface} (method signatures, no bodies) and nobody in this project implements it.
 * Spring Data JPA generates the implementation at runtime, on startup: it detects interfaces extending
 * {@code JpaRepository}, creates a proxy class, and registers it as a bean you can inject anywhere.
 *
 * <p>{@code JpaRepository<Customer, UUID>} uses generics: {@code <EntityType, IdType>}, like {@code Repository<T>} in
 * TS. It already provides {@code save}, {@code findById}, {@code findAll}, {@code deleteById}, {@code count}, paging...
 *
 * <p>Derived queries: Spring parses the METHOD NAME to build the query. {@code findByUserId(userId)} becomes
 * {@code select ... from customers where user_id = ?}. {@code existsBy...} returns a boolean, {@code findAllBy...}
 * with a {@code Pageable} adds limit/offset/order and returns a {@code Page}. A typo in the property name fails
 * application startup, so mistakes are caught early.
 */
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    // Interface methods are implicitly public and abstract; no modifiers needed.
    Optional<Customer> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    // Page<T> = one page of results plus total count / total pages (what a paginated API returns).
    // Pageable = page number, size and sort, usually built from ?page=0&size=20&sort=createdAt,desc.
    Page<Customer> findAllByKycStatus(KycStatus status, Pageable pageable);

    /**
     * Explicit JPQL example (queries entities/fields, not tables/columns).
     *
     * <p>{@code @Query} (runtime, parsed and validated by Spring Data at startup) supplies the query text instead of
     * deriving it from the method name. JPQL uses the entity name {@code Customer} and Java field names
     * ({@code kycStatus}); Hibernate translates it into SQL for {@code customers.kyc_status}.
     * {@code :status} is a named parameter, bound from the method parameter of the same name
     * (the build keeps parameter names for this).
     * "long" is a 64-bit integer primitive.
     */
    @Query("select count(c) from Customer c where c.kycStatus = :status")
    long countByStatus(KycStatus status);
}
