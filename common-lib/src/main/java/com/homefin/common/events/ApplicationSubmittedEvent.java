package com.homefin.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApplicationSubmittedEvent(
        UUID eventId,
        UUID applicationId,
        UUID customerUserId,
        BigDecimal financeAmount,
        int tenureMonths,
        Instant occurredAt) {
}
