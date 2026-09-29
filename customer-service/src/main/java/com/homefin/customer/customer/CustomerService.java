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
     * Deliberately NOT @Transactional: if saveAndFlush failed inside an outer transaction, that
     * transaction would be marked rollback-only and the commit would throw even though we caught
     * the exception. Each repository call runs in its own short transaction instead.
     */
    public void createFromRegistration(UserRegisteredEvent event) {
        if (customers.existsByUserId(event.userId())) {
            log.info("Customer already exists for userId={}, skipping duplicate event {}",
                    event.userId(), event.eventId());
            return;
        }
        try {
            customers.saveAndFlush(Customer.register(event.userId(), event.email(),
                    event.firstName(), event.lastName()));
            log.info("Customer profile created for userId={}", event.userId());
        } catch (DataIntegrityViolationException duplicate) {
            log.info("Concurrent duplicate for userId={}, ignoring", event.userId());
        }
    }

    @Transactional(readOnly = true)
    public Customer getByUserId(UUID userId) {
        return customers.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Customer for user", userId));
    }

    @Transactional(readOnly = true)
    public Customer getById(UUID id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Customer", id));
    }

    @Transactional(readOnly = true)
    public Page<Customer> search(KycStatus status, Pageable pageable) {
        return status == null ? customers.findAll(pageable) : customers.findAllByKycStatus(status, pageable);
    }

    /** No explicit save(): the entity is "managed", Hibernate flushes changes on commit (dirty checking). */
    @Transactional
    public Customer updateProfile(UUID userId, UpdateProfileRequest r) {
        Customer customer = getByUserId(userId);
        customer.updateProfile(r.firstName().trim(), r.lastName().trim(), r.phoneNumber(),
                r.dateOfBirth(), r.nationalId());
        return customer;
    }

    /**
     * NOTE: no @Transactional here on purpose - never hold a DB connection/transaction open while
     * waiting on a slow 3rd-party HTTP call. Read, call, then write in a short transaction.
     */
    public Customer verifyKyc(UUID userId) {
        Customer customer = getByUserId(userId);
        if (customer.getKycStatus() == KycStatus.VERIFIED) {
            return customer;
        }
        if (!customer.isProfileComplete()) {
            throw new BusinessRuleException("profile-incomplete",
                    "Complete phone number, date of birth and national ID before KYC");
        }
        KycVerificationResponse result = kycClient.verify(
                new KycVerificationRequest(customer.getFirstName(), customer.getLastName(),
                        customer.getDateOfBirth(), customer.getNationalId()),
                "kyc-" + customer.getId() + "-" + customer.getVersion());

        return saveKycResult(customer.getId(), result);
    }

    private Customer saveKycResult(UUID customerId, KycVerificationResponse result) {
        Customer fresh = getById(customerId);
        fresh.markKycResult(result.approved(), result.referenceId(), Instant.now());
        log.info("KYC result for customerId={} status={} score={}", customerId, result.status(), result.score());
        return customers.save(fresh); // optimistic locking (@Version) protects against lost updates
    }
}
