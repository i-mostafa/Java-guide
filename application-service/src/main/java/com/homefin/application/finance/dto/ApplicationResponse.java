package com.homefin.application.finance.dto;

import com.homefin.application.finance.ApplicationStatus;
import com.homefin.application.finance.PropertyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JSON response body for one finance application (what clients see).
 *
 * <p>Role: a DTO (data transfer object) filled by the MapStruct-generated ApplicationMapper from the
 * FinanceApplication entity, then serialized by Jackson. Keeping it separate from the entity means internal
 * columns (e.g. valuationReference, version) are not leaked and the API shape can evolve independently.
 * TS analogy: {@code type ApplicationResponse = { id: string; ...; createdAt: string }}.
 *
 * <p>Serialization: UUID becomes a string, BigDecimal a JSON number (exact digits), enums their name,
 * and {@code Instant} (java.time, a UTC point in time) an ISO-8601 string like "2026-01-31T10:15:30Z".
 */
// record: immutable; components become JSON properties with the same names.
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
