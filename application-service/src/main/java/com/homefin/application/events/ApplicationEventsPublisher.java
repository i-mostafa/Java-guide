package com.homefin.application.events;

import com.homefin.common.events.ApplicationStatusChangedEvent;
import com.homefin.common.events.ApplicationSubmittedEvent;
import com.homefin.common.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Bridges internal domain events to Kafka, but only AFTER the database transaction commits.
 *
 * <p>Why: if we sent to Kafka inside the transaction and the commit then failed, other services would
 * hear about an application that doesn't exist. Waiting for commit avoids that "phantom event" problem.
 * (It can still lose an event if the process dies right after commit; the fully robust version is the
 * transactional-outbox pattern.)
 *
 * <p>Who calls it: nobody calls these methods directly. Spring sees the {@code @TransactionalEventListener}
 * methods at startup and invokes them when a matching event object is published via
 * {@code ApplicationEventPublisher.publishEvent(...)} and the surrounding transaction commits.
 * TS analogy: {@code emitter.on('submitted', handler)} where emit is deferred until the DB commit resolves,
 * with kafkajs {@code producer.send} inside the handler.
 */
// @Slf4j (Lombok, compile time): generates a `log` field. @Component (runtime): register as a Spring bean.
// @RequiredArgsConstructor (Lombok, compile time): constructor for the final field -> Spring injects it.
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationEventsPublisher {

    // Generics: KafkaTemplate<String, Object> = template whose message KEY type is String and VALUE type
    // is Object (any object; serialized to JSON per application.yml). Like KafkaTemplate<K, V> in TS generics.
    // Spring Boot auto-configures this bean from the spring.kafka.* settings.
    private final KafkaTemplate<String, Object> kafka;

    // @TransactionalEventListener (runtime): Spring routes events to the listener by the PARAMETER TYPE
    // (here ApplicationDomainEvents.Submitted). phase = AFTER_COMMIT: run only once the transaction that
    // published the event has committed successfully; skipped if it rolls back.
    // Two methods can share the name "on" because Java supports overloading (same name, different parameter types).
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ApplicationDomainEvents.Submitted e) {
        // Record accessors are called like methods: e.applicationId(), not e.applicationId.
        // Instant = a UTC timestamp (java.time), like Date but immutable and unambiguous.
        send(Topics.APPLICATION_SUBMITTED, e.applicationId(), new ApplicationSubmittedEvent(
                UUID.randomUUID(), e.applicationId(), e.customerUserId(), e.financeAmount(), e.tenureMonths(),
                Instant.now()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ApplicationDomainEvents.StatusChanged e) {
        send(Topics.APPLICATION_STATUS_CHANGED, e.applicationId(), new ApplicationStatusChangedEvent(
                UUID.randomUUID(), e.applicationId(), e.customerUserId(), e.previousStatus(), e.newStatus(),
                e.reason(), Instant.now()));
    }

    /** Key = applicationId -> all events for one application stay ordered in one partition. */
    private void send(String topic, UUID key, Object payload) {
        // kafka.send(...) is asynchronous and returns a CompletableFuture (Java's Promise).
        // whenComplete((res, ex) -> ...) is like promise.then(res => ..., ex => ...) in one callback:
        // exactly one of res / ex is non-null. We don't block waiting for the broker.
        kafka.send(topic, key.toString(), payload).whenComplete((res, ex) -> {
            if (ex != null) {
                // Passing the exception as the LAST argument (with no {} for it) makes SLF4J log its stack trace.
                log.error("Failed to publish {} to {}", payload.getClass().getSimpleName(), topic, ex);
            } else {
                log.info("Published {} key={} partition={} offset={}", payload.getClass().getSimpleName(), key,
                        res.getRecordMetadata().partition(), res.getRecordMetadata().offset());
            }
        });
    }
}
