package com.homefin.customer.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    Page<Customer> findAllByKycStatus(KycStatus status, Pageable pageable);

    /** Explicit JPQL example (queries entities/fields, not tables/columns). */
    @Query("select count(c) from Customer c where c.kycStatus = :status")
    long countByStatus(KycStatus status);
}
