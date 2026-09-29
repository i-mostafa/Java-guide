package com.homefin.auth.events;

import com.homefin.common.events.Topics;
import com.homefin.common.events.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Bridges the in-process domain event to Kafka, only after the DB commit succeeds
 * (so we never announce a user that was rolled back).
 *
 * Trade-off: if the app crashes between commit and publish, the event is lost.
 * For guaranteed delivery use the Transactional Outbox pattern (see the guide, section Kafka).
 *
 * <p>Flow: AuthService.register publishes a {@code UserRegisteredDomainEvent} inside its
 * transaction; Spring holds it back and, once the transaction commits, calls {@code on(...)} here,
 * which sends the public {@code UserRegisteredEvent} to Kafka. Node analogy:
 * {@code await db.transaction(...); producer.send({ topic, messages: [{ key, value }] })}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventsPublisher {

    // KafkaTemplate<K, V>: Spring's Kafka producer wrapper (like a kafkajs producer), auto-configured
    // from spring.kafka.* in application.yml. Generic arguments: key type String, value type Object
    // (the value is serialized to JSON by JsonSerializer).
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // @TransactionalEventListener (Spring, runtime): subscribes this method to UserRegisteredDomainEvent
    // (the parameter type decides which events it receives) and defers the call until the surrounding
    // transaction reaches the given phase - here AFTER_COMMIT. If the transaction rolls back, it never runs.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserRegisteredDomainEvent e) {
        var event = new UserRegisteredEvent(UUID.randomUUID(), e.userId(), e.email(),
                e.firstName(), e.lastName(), Instant.now());

        // Key = userId -> all events of one user land in the same partition (ordering guarantee).
        // send(...) is asynchronous and returns a CompletableFuture (Java's Promise).
        // whenComplete((result, ex) -> ...) is like .then(ok, err) combined: exactly one of the two
        // arguments is non-null.
        kafkaTemplate.send(Topics.USER_REGISTERED, e.userId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish UserRegisteredEvent userId={}", e.userId(), ex);
                    } else {
                        log.info("Published UserRegisteredEvent userId={} offset={}", e.userId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
