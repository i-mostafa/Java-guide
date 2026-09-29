package com.homefin.application.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Data access for FinanceApplication rows. There is NO implementation class anywhere - on purpose.
 *
 * <p>Role: at startup Spring Data JPA finds this interface and generates a proxy implementation (a bean you
 * can inject). By extending {@code JpaRepository<FinanceApplication, UUID>} (entity type, id type) you inherit
 * save, findById, findAll(pageable), delete, count... TS analogy: TypeORM's {@code Repository<FinanceApplication>},
 * obtained without writing a class.
 *
 * <p>Derived queries: Spring Data PARSES the method names below into queries at startup:
 * {@code findAllBy} + {@code CustomerUserId} becomes {@code WHERE customer_user_id = ?}; a {@code Pageable}
 * parameter adds ORDER BY / LIMIT / OFFSET plus a COUNT query for the total. A typo in a property name makes
 * the app fail to start, so mistakes are caught early.
 */
// interface X extends Y<A, B>: inherits Y's methods with the generic type parameters filled in.
public interface FinanceApplicationRepository extends JpaRepository<FinanceApplication, UUID> {

    // Page<T> = one page of results plus total count/pages metadata.
    Page<FinanceApplication> findAllByCustomerUserId(UUID customerUserId, Pageable pageable);

    Page<FinanceApplication> findAllByStatus(ApplicationStatus status, Pageable pageable);

    // "exists...By A And B And C In" -> SELECT whether any row matches
    // customer_user_id = ? AND property_reference = ? AND status IN (...).
    // java.util.Collection is written fully qualified (package + name) instead of imported; both are equivalent.
    boolean existsByCustomerUserIdAndPropertyReferenceAndStatusIn(UUID customerUserId, String propertyReference,
                                                                  java.util.Collection<ApplicationStatus> statuses);
}
