package com.homefin.application.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FinanceApplicationRepository extends JpaRepository<FinanceApplication, UUID> {

    Page<FinanceApplication> findAllByCustomerUserId(UUID customerUserId, Pageable pageable);

    Page<FinanceApplication> findAllByStatus(ApplicationStatus status, Pageable pageable);

    boolean existsByCustomerUserIdAndPropertyReferenceAndStatusIn(UUID customerUserId, String propertyReference,
                                                                  java.util.Collection<ApplicationStatus> statuses);
}
