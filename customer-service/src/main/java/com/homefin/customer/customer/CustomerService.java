package com.homefin.customer.customer;

import com.homefin.common.error.BusinessRuleException;
import com.homefin.common.error.NotFoundException;
import com.homefin.common.events.UserRegisteredEvent;
import com.homefin.customer.customer.dto.UpdateProfileRequest;
import com.homefin.customer.kyc.KycClient;
import com.homefin.customer.kyc.KycVerificationRequest;
import com.homefin.customer.kyc.KycVerificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Business logic for customers: the NestJS {@code @Injectable()} service that controllers and Kafka listeners call.
 * It owns transactions and orchestrates the repository (DB) and {@link KycClient} (3rd-party HTTP).
 *
 * <ul>
 *   <li>{@code @Slf4j} (Lombok, compile time): generates the {@code log} logger field.</li>
 *   <li>{@code @Service} (runtime): same as {@code @Component} (a singleton bean created at startup), with a name
 *       that documents it's the service layer.</li>
 *   <li>{@code @RequiredArgsConstructor} (Lombok, compile time): constructor for the final fields; Spring injects
 *       the Spring Data repository and the KycClient bean through it.</li>
 * </ul>
 *
 * <p>{@code @Transactional} (runtime, AOP) on a method: Spring doesn't hand out this class directly but a PROXY
 * (a generated subclass wrapping it). Calling a {@code @Transactional} method through the proxy opens a DB
 * transaction, runs the method, then commits, or rolls back if a RuntimeException escapes. Like wrapping the body
 * in {@code dataSource.transaction(async (em) => ...)}. Caveat: the proxy only sees calls coming from OTHER beans. A
 * call from one method of this class to another ("self-invocation", e.g. {@code verifyKyc} calling
 * {@code getByUserId}) bypasses the proxy, so the callee's annotation does not apply to that call.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customers;
    private final KycClient kycClient;

    /**
     * Idempotent consumer: Kafka guarantees AT-LEAST-ONCE delivery, so the same event may arrive
     * twice. We check first, and the unique index on user_id is the final safety net.
     *
     * <p>Deliberately NOT {@code @Transactional}: if saveAndFlush failed inside an outer transaction, that
     * transaction would be marked rollback-only and the commit would throw even though we caught
     * the exception. Each repository call runs in its own short transaction instead.
     */
    public void createFromRegistration(UserRegisteredEvent event) {
        if (customers.existsByUserId(event.userId())) {
            log.info("Customer already exists for userId={}, skipping duplicate event {}",
                    event.userId(), event.eventId());
            return;
        }
        // try/catch works like JS, but each catch names the exception TYPE it handles; others propagate.
        try {
            // saveAndFlush = INSERT immediately (not at commit time), so a unique-index violation surfaces here.
            customers.saveAndFlush(Customer.register(event.userId(), event.email(),
                    event.firstName(), event.lastName()));
            log.info("Customer profile created for userId={}", event.userId());
        } catch (DataIntegrityViolationException duplicate) {
            // Two instances processed the same event concurrently; the other one won. Swallowing is correct here.
            log.info("Concurrent duplicate for userId={}, ignoring", event.userId());
        }
    }

    // readOnly = true: a hint that lets Hibernate skip dirty checking and lets the driver/DB optimize; no writes.
    @Transactional(readOnly = true)
    public Customer getByUserId(UUID userId) {
        // Optional.orElseThrow(supplier): return the value, or throw the exception the lambda creates.
        // "() -> new X(...)" is a zero-argument lambda, like () => new X(...) in JS.
        return customers.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Customer for user", userId));
    }

    @Transactional(readOnly = true)
    public Customer getById(UUID id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Customer", id));
    }

    @Transactional(readOnly = true)
    public Page<Customer> search(KycStatus status, Pageable pageable) {
        // findAll(pageable) is inherited from JpaRepository; findAllByKycStatus is a derived query we declared.
        return status == null ? customers.findAll(pageable) : customers.findAllByKycStatus(status, pageable);
    }

    /** No explicit save(): the entity is "managed", Hibernate flushes changes on commit (dirty checking). */
    // Entities loaded inside a transaction are tracked by Hibernate's persistence context; when the transaction
    // commits it compares them with their loaded state and issues UPDATEs for what changed.
    @Transactional
    public Customer updateProfile(UUID userId, UpdateProfileRequest r) {
        // Self-invocation: this getByUserId call runs inside THIS method's (read-write) transaction.
        Customer customer = getByUserId(userId);
        // r.firstName() is the record accessor; .trim() removes surrounding whitespace, like String.prototype.trim.
        customer.updateProfile(r.firstName().trim(), r.lastName().trim(), r.phoneNumber(),
                r.dateOfBirth(), r.nationalId());
        return customer;
    }

    /**
     * NOTE: no {@code @Transactional} here on purpose - never hold a DB connection/transaction open while
     * waiting on a slow 3rd-party HTTP call. Read, call, then write in a short transaction.
     */
    public Customer verifyKyc(UUID userId) {
        // Self-invocation bypasses the proxy, so this read runs without a surrounding transaction; the repository
        // method itself is transactional, so the query still gets its own short transaction.
        Customer customer = getByUserId(userId);
        if (customer.getKycStatus() == KycStatus.VERIFIED) {
            return customer;
        }
        if (!customer.isProfileComplete()) {
            throw new BusinessRuleException("profile-incomplete",
                    "Complete phone number, date of birth and national ID before KYC");
        }
        // kycClient is another bean, so this call DOES go through its proxy (Retry + CircuitBreaker apply).
        // The idempotency key changes only when the customer row changes (version), so retries reuse it.
        // String + anything concatenates, like JS.
        KycVerificationResponse result = kycClient.verify(
                new KycVerificationRequest(customer.getFirstName(), customer.getLastName(),
                        customer.getDateOfBirth(), customer.getNationalId()),
                "kyc-" + customer.getId() + "-" + customer.getVersion());

        return saveKycResult(customer.getId(), result);
    }

    // Reload a fresh copy (the row may have changed during the slow HTTP call) and save it.
    // repository.save() is transactional by itself, which gives the "short write transaction" described above.
    private Customer saveKycResult(UUID customerId, KycVerificationResponse result) {
        Customer fresh = getById(customerId);
        // Instant.now() = current UTC timestamp, like new Date().
        fresh.markKycResult(result.approved(), result.referenceId(), Instant.now());
        log.info("KYC result for customerId={} status={} score={}", customerId, result.status(), result.score());
        return customers.save(fresh); // optimistic locking (@Version) protects against lost updates
    }
}
