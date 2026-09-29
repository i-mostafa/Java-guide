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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventsPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserRegisteredDomainEvent e) {
        var event = new UserRegisteredEvent(UUID.randomUUID(), e.userId(), e.email(),
                e.firstName(), e.lastName(), Instant.now());

        // Key = userId -> all events of one user land in the same partition (ordering guarantee).
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
