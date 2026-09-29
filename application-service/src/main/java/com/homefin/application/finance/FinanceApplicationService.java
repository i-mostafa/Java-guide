package com.homefin.application.finance;

import com.homefin.application.config.FinanceProperties;
import com.homefin.application.customer.CustomerGateway;
import com.homefin.application.customer.CustomerSummary;
import com.homefin.application.events.ApplicationDomainEvents;
import com.homefin.application.finance.dto.CreateApplicationRequest;
import com.homefin.application.finance.dto.UpdateStatusRequest;
import com.homefin.application.valuation.ValuationGateway;
import com.homefin.application.valuation.ValuationRequest;
import com.homefin.application.valuation.ValuationResponse;
import com.homefin.common.error.BusinessRuleException;
import com.homefin.common.error.ConflictException;
import com.homefin.common.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceApplicationService {

    private final FinanceApplicationRepository applications;
    private final CustomerGateway customerGateway;
    private final ValuationGateway valuationGateway;
    private final InstallmentCalculator calculator;
    private final FinanceProperties financeProps;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;

    /**
     * Orchestration:
     *   1. sync call to customer-service (Feign)       -> is the customer KYC verified?
     *   2. sync call to 3rd-party valuation (RestClient) -> what is the property worth?
     *   3. business rules (finance-to-value), installment calculation
     *   4. short DB transaction + event (published to Kafka after commit)
     * Remote calls happen OUTSIDE the transaction; TransactionTemplate is the programmatic
     * alternative to @Transactional (which would not work on a private method of this same class).
     */
    public FinanceApplication submit(UUID userId, CreateApplicationRequest req) {
        CustomerSummary customer = customerGateway.currentCustomer();
        if (!customer.isKycVerified()) {
            throw new BusinessRuleException("kyc-required", "Identity verification (KYC) must be completed first");
        }
        if (applications.existsByCustomerUserIdAndPropertyReferenceAndStatusIn(userId, req.propertyReference(),
                EnumSet.of(ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW))) {
            throw new ConflictException("duplicate-application",
                    "An open application already exists for this property");
        }

        ValuationResponse valuation = valuationGateway.estimate(
                new ValuationRequest(req.propertyReference(), req.city(), req.propertyType().name()));

        // Lend against the LOWER of declared price and independent valuation.
        BigDecimal effectiveValue = req.propertyValue().min(valuation.estimatedValue());
        BigDecimal ftv = req.financeAmount().divide(effectiveValue, 4, RoundingMode.HALF_UP);
        if (ftv.compareTo(financeProps.maxFinanceToValue()) > 0) {
            throw new BusinessRuleException("ftv-exceeded",
                    "Finance-to-value %s exceeds the maximum %s (valuation %s)"
                            .formatted(ftv, financeProps.maxFinanceToValue(), valuation.estimatedValue()));
        }

        BigDecimal installment = calculator.monthlyInstallment(
                req.financeAmount(), financeProps.annualProfitRate(), req.tenureMonths());

        return tx.execute(status -> {
            FinanceApplication saved = applications.save(FinanceApplication.builder()
                    .customerUserId(userId)
                    .customerId(customer.id())
                    .propertyReference(req.propertyReference())
                    .city(req.city())
                    .propertyType(req.propertyType())
                    .declaredValue(req.propertyValue())
                    .valuationAmount(valuation.estimatedValue())
                    .valuationReference(valuation.valuationId())
                    .financeAmount(req.financeAmount())
                    .tenureMonths(req.tenureMonths())
                    .profitRate(financeProps.annualProfitRate())
                    .monthlyInstallment(installment)
                    .build());
            events.publishEvent(new ApplicationDomainEvents.Submitted(saved.getId(), userId,
                    saved.getFinanceAmount(), saved.getTenureMonths()));
            log.info("Application submitted id={} ftv={} installment={}", saved.getId(), ftv, installment);
            return saved;
        });
    }

    @Transactional(readOnly = true)
    public FinanceApplication get(UUID id) {
        return applications.findById(id).orElseThrow(() -> new NotFoundException("Application", id));
    }

    @Transactional(readOnly = true)
    public Page<FinanceApplication> listForCustomer(UUID userId, Pageable pageable) {
        return applications.findAllByCustomerUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FinanceApplication> listByStatus(ApplicationStatus status, Pageable pageable) {
        return status == null ? applications.findAll(pageable) : applications.findAllByStatus(status, pageable);
    }

    @Transactional
    public FinanceApplication changeStatus(UUID id, UpdateStatusRequest req) {
        FinanceApplication app = get(id);
        ApplicationStatus previous = app.changeStatus(req.status(), req.reason());
        events.publishEvent(new ApplicationDomainEvents.StatusChanged(app.getId(), app.getCustomerUserId(),
                previous.name(), req.status().name(), req.reason()));
        return app; // dirty checking persists the change on commit
    }
}
