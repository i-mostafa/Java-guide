package com.homefin.application.events;

import java.math.BigDecimal;
import java.util.UUID;

/** Internal Spring events, translated to Kafka messages after commit. */
public final class ApplicationDomainEvents {

    private ApplicationDomainEvents() {
    }

    public record Submitted(UUID applicationId, UUID customerUserId, BigDecimal financeAmount, int tenureMonths) {
    }

    public record StatusChanged(UUID applicationId, UUID customerUserId, String previousStatus,
                                String newStatus, String reason) {
    }
}
