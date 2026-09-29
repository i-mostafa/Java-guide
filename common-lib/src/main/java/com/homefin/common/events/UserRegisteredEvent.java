package com.homefin.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by auth-service after a user signs up; consumed by customer-service.
 * Java records = immutable DTOs (like a TS `readonly` type), perfect for events.
 *
 * <p>The parameter list after the record name IS the field list. Java generates a constructor
 * {@code new UserRegisteredEvent(eventId, userId, ...)}, accessors like {@code email()}, and
 * value-based equals/hashCode/toString. The empty braces mean "no extra methods".
 */
public record UserRegisteredEvent(
        UUID eventId,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        Instant occurredAt) {
}
