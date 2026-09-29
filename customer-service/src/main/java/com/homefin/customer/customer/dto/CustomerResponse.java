package com.homefin.customer.customer.dto;

import com.homefin.customer.customer.KycStatus;

// Instant = a point on the UTC timeline (like a JS Date / epoch millis); serialized as ISO-8601 "2026-01-01T10:00:00Z".
import java.time.Instant;
// LocalDate = a date with no time and no zone (birthdays); serialized as "1990-05-17".
import java.time.LocalDate;
// UUID = a 128-bit id type (instead of passing ids around as plain strings).
import java.util.UUID;

/**
 * The JSON body returned by the customer REST endpoints (a response DTO, like a NestJS response class or a
 * TS {@code type CustomerResponse = {...}}).
 *
 * <p>A {@code record} is an immutable data carrier: the compiler generates the constructor, accessors
 * ({@code id()}, {@code email()}...), {@code equals}/{@code hashCode}/{@code toString}. Jackson turns it into JSON
 * using the component names as keys.
 *
 * <p>Why not return the {@code Customer} entity directly? The entity is the DB shape (with version, raw national
 * id, lazy relations...). A dedicated DTO lets the API evolve independently and never leaks internal fields;
 * note {@code nationalIdMasked} instead of the raw value. {@code CustomerMapper} builds it from the entity.
 */
public record CustomerResponse(
        UUID id,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        LocalDate dateOfBirth,
        String nationalIdMasked,
        KycStatus kycStatus,
        Instant kycCheckedAt,
        Instant createdAt) {
}
