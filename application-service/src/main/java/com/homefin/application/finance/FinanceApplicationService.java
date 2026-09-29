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

/**
 * Business logic for finance applications: the use cases the controller calls.
 *
 * <p>Role: the "service layer". It orchestrates remote calls (customer-service, valuation provider),
 * applies business rules, persists through the repository and publishes domain events. It knows nothing
 * about HTTP. TS analogy: a NestJS {@code @Injectable()} service injected into the controller.
 *
 * <p>Transactions: {@code @Transactional} methods are wrapped by an AOP PROXY that Spring puts in front of
 * this bean: the proxy opens a DB transaction, calls the real method, then commits (or rolls back if an
 * unchecked exception escapes). TS analogy: wrapping the method body in
 * {@code dataSource.transaction(async manager => ...)}. Because the proxy sits OUTSIDE the object, it only
 * sees calls coming from other beans: a call from one method of this class to another ({@code this.x()})
 * skips the proxy and gets no transaction. That is why {@code submit} uses TransactionTemplate instead.
 */
// @Slf4j (Lombok, compile time): generates the `log` field.
@Slf4j
// @Service (runtime): same as @Component (a Spring-managed singleton bean); the name just documents the role.
@Service
// @RequiredArgsConstructor (Lombok, compile time): a constructor with all 7 final fields -> Spring injects them.
// Tests can call that constructor directly with mocks (see FinanceApplicationServiceTest).
@RequiredArgsConstructor
public class FinanceApplicationService {

    private final FinanceApplicationRepository applications;
    private final CustomerGateway customerGateway;
    private final ValuationGateway valuationGateway;
    private final InstallmentCalculator calculator;
    private final FinanceProperties financeProps;
    // Spring's in-process event bus (like an EventEmitter); listeners are in ApplicationEventsPublisher.
    private final ApplicationEventPublisher events;
    // Auto-configured by Spring Boot; runs a lambda inside a transaction.
    private final TransactionTemplate tx;

    /**
     * Orchestration:
     *   1. sync call to customer-service (Feign)       -> is the customer KYC verified?
     *   2. sync call to 3rd-party valuation (RestClient) -> what is the property worth?
     *   3. business rules (finance-to-value), installment calculation
     *   4. short DB transaction + event (published to Kafka after commit)
     * Remote calls happen OUTSIDE the transaction; TransactionTemplate is the programmatic
     * alternative to {@code @Transactional} (which would not work on a private method of this same class).
     *
     * <p>Why outside: a DB transaction holds a connection from a small pool; keeping it open while waiting
     * seconds for HTTP calls would exhaust the pool under load.
     */
    public FinanceApplication submit(UUID userId, CreateApplicationRequest req) {
        CustomerSummary customer = customerGateway.currentCustomer();
        if (!customer.isKycVerified()) {
            throw new BusinessRuleException("kyc-required", "Identity verification (KYC) must be completed first");
        }
        // EnumSet.of(...) = a compact, fast Set specialised for enum values.
        if (applications.existsByCustomerUserIdAndPropertyReferenceAndStatusIn(userId, req.propertyReference(),
                EnumSet.of(ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW))) {
            throw new ConflictException("duplicate-application",
                    "An open application already exists for this property");
        }

        // Retries / circuit breaking happen inside ValuationGateway's proxy. .name() = the enum constant's name.
        ValuationResponse valuation = valuationGateway.estimate(
                new ValuationRequest(req.propertyReference(), req.city(), req.propertyType().name()));

        // Lend against the LOWER of declared price and independent valuation.
        // BigDecimal has no operator overloading: use methods (min, divide, multiply, compareTo) instead of < / * .
        BigDecimal effectiveValue = req.propertyValue().min(valuation.estimatedValue());
        // divide(x, 4, HALF_UP) = divide keeping 4 decimal places, rounding half up. BigDecimal forces you to
        // choose a scale and rounding for division, because 1/3 has no exact decimal representation.
        BigDecimal ftv = req.financeAmount().divide(effectiveValue, 4, RoundingMode.HALF_UP);
        // compareTo returns -1 / 0 / 1 (a > b  <=>  a.compareTo(b) > 0). Don't use equals() for numeric
        // comparison: BigDecimal.equals also compares the scale (0.80 vs 0.8).
        if (ftv.compareTo(financeProps.maxFinanceToValue()) > 0) {
            throw new BusinessRuleException("ftv-exceeded",
                    "Finance-to-value %s exceeds the maximum %s (valuation %s)"
                            .formatted(ftv, financeProps.maxFinanceToValue(), valuation.estimatedValue()));
        }

        BigDecimal installment = calculator.monthlyInstallment(
                req.financeAmount(), financeProps.annualProfitRate(), req.tenureMonths());

        // tx.execute(status -> { ... }) runs the lambda inside a transaction and returns its result;
        // commit happens when the lambda returns, rollback if it throws. `status` (unused here) would allow
        // status.setRollbackOnly().
        return tx.execute(status -> {
            // Lombok-generated builder: FinanceApplication.builder().field(value)...build().
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
            // Published now, but the @TransactionalEventListener only fires after this transaction commits.
            events.publishEvent(new ApplicationDomainEvents.Submitted(saved.getId(), userId,
                    saved.getFinanceAmount(), saved.getTenureMonths()));
            log.info("Application submitted id={} ftv={} installment={}", saved.getId(), ftv, installment);
            return saved;
        });
    }

    // @Transactional(readOnly = true) (runtime, via the AOP proxy): run in a read-only transaction; lets
    // Hibernate skip dirty checking and lets the driver/DB optimise.
    @Transactional(readOnly = true)
    public FinanceApplication get(UUID id) {
        // findById returns Optional<FinanceApplication>: a box that is either empty or holds a value (Java's
        // explicit alternative to `T | undefined`). orElseThrow(supplier) unwraps it or throws the exception
        // created by the lambda `() -> new NotFoundException(...)` (-> HTTP 404).
        return applications.findById(id).orElseThrow(() -> new NotFoundException("Application", id));
    }

    @Transactional(readOnly = true)
    public Page<FinanceApplication> listForCustomer(UUID userId, Pageable pageable) {
        return applications.findAllByCustomerUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FinanceApplication> listByStatus(ApplicationStatus status, Pageable pageable) {
        // Ternary operator, same as in TS. `== null` is the right way to test for null in Java.
        return status == null ? applications.findAll(pageable) : applications.findAllByStatus(status, pageable);
    }

    // @Transactional (runtime, AOP proxy): read-write transaction around the whole method. The call to get(id)
    // below is a self-invocation, so ITS annotation is ignored; it simply joins this transaction.
    @Transactional
    public FinanceApplication changeStatus(UUID id, UpdateStatusRequest req) {
        FinanceApplication app = get(id);
        ApplicationStatus previous = app.changeStatus(req.status(), req.reason());
        events.publishEvent(new ApplicationDomainEvents.StatusChanged(app.getId(), app.getCustomerUserId(),
                previous.name(), req.status().name(), req.reason()));
        return app; // dirty checking persists the change on commit
    }
}
