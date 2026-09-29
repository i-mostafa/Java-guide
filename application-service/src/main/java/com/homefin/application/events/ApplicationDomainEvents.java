package com.homefin.application.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Internal Spring events, translated to Kafka messages after commit.
 *
 * <p>Role: FinanceApplicationService publishes these in-process with {@code ApplicationEventPublisher}
 * (like Node's {@code EventEmitter.emit}); ApplicationEventsPublisher listens and forwards them to Kafka
 * once the DB transaction has committed. Keeping them separate from the Kafka event classes in common-lib
 * means the internal shape can change without breaking other services.
 *
 * <p>This class is just a namespace grouping two nested records, similar to a TS {@code namespace} or a
 * module exporting two types. Used as {@code ApplicationDomainEvents.Submitted}.
 */
// final class = cannot be subclassed (like a sealed/non-extendable class).
public final class ApplicationDomainEvents {

    // A private constructor prevents anyone from doing `new ApplicationDomainEvents()`:
    // this is the standard Java idiom for a "holder"/utility class that is never instantiated.
    private ApplicationDomainEvents() {
    }

    // Nested records are implicitly "static": they do not need an instance of the outer class.
    // int = primitive 32-bit integer (cannot be null, unlike the wrapper type Integer).
    public record Submitted(UUID applicationId, UUID customerUserId, BigDecimal financeAmount, int tenureMonths) {
    }

    public record StatusChanged(UUID applicationId, UUID customerUserId, String previousStatus,
                                String newStatus, String reason) {
    }
}
