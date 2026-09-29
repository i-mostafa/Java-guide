package com.homefin.common.events;

import java.time.Instant;
import java.util.UUID;

public record ApplicationStatusChangedEvent(
        UUID eventId,
        UUID applicationId,
        UUID customerUserId,
        String previousStatus,
        String newStatus,
        String reason,
        Instant occurredAt) {
}
