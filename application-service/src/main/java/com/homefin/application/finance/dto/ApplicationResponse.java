package com.homefin.application.finance.dto;

import com.homefin.application.finance.ApplicationStatus;
import com.homefin.application.finance.PropertyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID customerUserId,
        String propertyReference,
        String city,
        PropertyType propertyType,
        BigDecimal declaredValue,
        BigDecimal valuationAmount,
        BigDecimal financeAmount,
        int tenureMonths,
        BigDecimal profitRate,
        BigDecimal monthlyInstallment,
        ApplicationStatus status,
        String statusReason,
        Instant createdAt,
        Instant updatedAt) {
}
