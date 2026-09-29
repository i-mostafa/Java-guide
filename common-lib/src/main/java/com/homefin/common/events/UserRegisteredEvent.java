package com.homefin.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by auth-service after a user signs up; consumed by customer-service.
 * Java records = immutable DTOs (like a TS `readonly` type), perfect for events.
 */
public record UserRegisteredEvent(
        UUID eventId,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        Instant occurredAt) {
}
