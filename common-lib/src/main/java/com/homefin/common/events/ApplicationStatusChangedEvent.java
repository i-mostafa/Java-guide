package com.homefin.common.events;

// java.time.Instant = a UTC timestamp (a point on the timeline), like a JS Date / ISO string.
import java.time.Instant;
// UUID is a built-in JDK type (no library needed).
import java.util.UUID;

/**
 * Kafka event published by application-service whenever a finance application moves to a new
 * status (e.g. SUBMITTED -> APPROVED). Part of the cross-service contract, so it lives here.
 *
 * <p>A {@code record} is a compact, immutable data class: the header below declares the fields
 * AND generates a constructor, accessor methods ({@code newStatus()} - no "get" prefix),
 * {@code equals}, {@code hashCode} and {@code toString}. TS analogy:
 * {@code type ApplicationStatusChangedEvent = Readonly<{ eventId: string; ... }>} plus a
 * constructor. Jackson serializes it to/from JSON for Kafka.
 */
public record ApplicationStatusChangedEvent(
        UUID eventId,
        UUID applicationId,
        UUID customerUserId,
        String previousStatus,
        String newStatus,
        String reason,
        Instant occurredAt) {
}
