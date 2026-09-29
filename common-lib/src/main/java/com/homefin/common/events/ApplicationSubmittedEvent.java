package com.homefin.common.events;

// BigDecimal = arbitrary-precision decimal number. Always used for money in Java, because
// double (like JS number) cannot represent values such as 0.1 exactly. Similar to decimal.js.
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event published by application-service when a customer submits a finance application.
 *
 * <p>A {@code record}: an immutable data carrier with auto-generated constructor, accessors
 * ({@code financeAmount()}), equals/hashCode/toString. Like a readonly TS type for an event payload.
 */
public record ApplicationSubmittedEvent(
        UUID eventId,
        UUID applicationId,
        UUID customerUserId,
        BigDecimal financeAmount,
        // int is a primitive 32-bit integer (lowercase = primitive, cannot be null).
        // Its "boxed" object form Integer can be null. TS has only "number".
        int tenureMonths,
        Instant occurredAt) {
}
