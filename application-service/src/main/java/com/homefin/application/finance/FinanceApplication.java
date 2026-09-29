package com.homefin.application.finance;

import com.homefin.common.error.BusinessRuleException;
import com.homefin.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * JPA entity = one row of the {@code finance_applications} table (created by the Flyway script V1).
 *
 * <p>Role: the core domain object. Hibernate (the JPA implementation) maps fields to columns, loads rows
 * into objects and writes changes back. TS analogy: a TypeORM {@code @Entity()} class with {@code @Column()}
 * fields. The id, createdAt, updatedAt and version (optimistic locking) columns come from common-lib's
 * BaseEntity superclass.
 *
 * <p>Design: there are no setters. New objects are created through the builder (a static factory) and
 * the status only changes through {@code changeStatus}, which enforces the state machine. This keeps the
 * business rules inside the entity ("rich domain model") instead of letting any code poke fields.
 */
// @Entity (runtime, Hibernate): this class is persistent; Hibernate scans it at startup.
@Entity
// @Table (runtime): explicit table name (the default would be derived from the class name).
@Table(name = "finance_applications")
// @Getter (Lombok, compile time): generates a public getter for every field: getCity(), getStatus(), ...
@Getter
// @NoArgsConstructor (Lombok, compile time): generates an empty constructor. JPA REQUIRES one so Hibernate can
// instantiate objects when loading rows. access = PROTECTED hides it from normal application code.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// extends = inherit fields/methods from BaseEntity (single inheritance, like TS "extends").
public class FinanceApplication extends BaseEntity {

    // @Column (runtime): maps a field to a column. nullable/length/precision describe the column (also
    // checked by ddl-auto=validate at startup). updatable = false: Hibernate never includes it in UPDATEs.
    @Column(name = "customer_user_id", nullable = false, updatable = false)
    private UUID customerUserId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "property_reference", nullable = false, length = 50)
    private String propertyReference;

    @Column(nullable = false, length = 100)
    private String city;

    // @Enumerated(EnumType.STRING) (runtime): store the enum's NAME ("VILLA"). The default, ORDINAL,
    // stores its position (0, 1, 2) and silently corrupts data if someone reorders the constants.
    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 20)
    private PropertyType propertyType;

    /**
     * Money is ALWAYS BigDecimal (never double) - 0.1 + 0.2 != 0.3 in floating point.
     * precision = total digits, scale = digits after the decimal point: numeric(14, 2) in SQL.
     */
    @Column(name = "declared_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal declaredValue;

    @Column(name = "valuation_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal valuationAmount;

    @Column(name = "valuation_reference", length = 64)
    private String valuationReference;

    @Column(name = "finance_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal financeAmount;

    @Column(name = "tenure_months", nullable = false)
    private int tenureMonths;

    @Column(name = "profit_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal profitRate;

    @Column(name = "monthly_installment", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyInstallment;

    // Field initializer: every new FinanceApplication starts as SUBMITTED.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    /**
     * Lombok {@code @Builder} on a constructor/static factory = readable construction of many fields.
     *
     * <p>At compile time Lombok generates a nested {@code FinanceApplicationBuilder} class and a static
     * {@code builder()} method, so callers write
     * {@code FinanceApplication.builder().city("Dubai").tenureMonths(300).build()}: named arguments instead
     * of a 12-parameter positional call (like passing one options object in TS). {@code build()} calls this
     * private method, which is why it may stay private.
     */
    @Builder
    // private static: a class-level factory method, not callable from outside except through the builder.
    private static FinanceApplication submit(UUID customerUserId, UUID customerId, String propertyReference,
                                             String city, PropertyType propertyType, BigDecimal declaredValue,
                                             BigDecimal valuationAmount, String valuationReference,
                                             BigDecimal financeAmount, int tenureMonths, BigDecimal profitRate,
                                             BigDecimal monthlyInstallment) {
        FinanceApplication a = new FinanceApplication();
        // Code inside the class may assign private fields of any instance of the same class.
        a.customerUserId = customerUserId;
        a.customerId = customerId;
        a.propertyReference = propertyReference;
        a.city = city;
        a.propertyType = propertyType;
        a.declaredValue = declaredValue;
        a.valuationAmount = valuationAmount;
        a.valuationReference = valuationReference;
        a.financeAmount = financeAmount;
        a.tenureMonths = tenureMonths;
        a.profitRate = profitRate;
        a.monthlyInstallment = monthlyInstallment;
        return a;
    }

    /**
     * The only way to change the status. Returns the previous status so the caller can publish an event.
     * No explicit save is needed: inside a transaction Hibernate detects the modified fields ("dirty checking")
     * and issues the UPDATE at commit.
     */
    public ApplicationStatus changeStatus(ApplicationStatus next, String reason) {
        if (!status.canTransitionTo(next)) {
            // "...%s...".formatted(a, b) = String.format: fills %s placeholders, like a JS template literal.
            throw new BusinessRuleException("invalid-status-transition",
                    "Cannot move application from %s to %s".formatted(status, next));
        }
        // this.status = the field of the current object (same meaning as `this` in a TS class).
        ApplicationStatus previous = this.status;
        this.status = next;
        this.statusReason = reason;
        return previous;
    }
}
