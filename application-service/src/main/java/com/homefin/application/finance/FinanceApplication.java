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

@Entity
@Table(name = "finance_applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinanceApplication extends BaseEntity {

    @Column(name = "customer_user_id", nullable = false, updatable = false)
    private UUID customerUserId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "property_reference", nullable = false, length = 50)
    private String propertyReference;

    @Column(nullable = false, length = 100)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 20)
    private PropertyType propertyType;

    /** Money is ALWAYS BigDecimal (never double) - 0.1 + 0.2 != 0.3 in floating point. */
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    /** Lombok @Builder on a constructor/static factory = readable construction of many fields. */
    @Builder
    private static FinanceApplication submit(UUID customerUserId, UUID customerId, String propertyReference,
                                             String city, PropertyType propertyType, BigDecimal declaredValue,
                                             BigDecimal valuationAmount, String valuationReference,
                                             BigDecimal financeAmount, int tenureMonths, BigDecimal profitRate,
                                             BigDecimal monthlyInstallment) {
        FinanceApplication a = new FinanceApplication();
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

    public ApplicationStatus changeStatus(ApplicationStatus next, String reason) {
        if (!status.canTransitionTo(next)) {
            throw new BusinessRuleException("invalid-status-transition",
                    "Cannot move application from %s to %s".formatted(status, next));
        }
        ApplicationStatus previous = this.status;
        this.status = next;
        this.statusReason = reason;
        return previous;
    }
}
