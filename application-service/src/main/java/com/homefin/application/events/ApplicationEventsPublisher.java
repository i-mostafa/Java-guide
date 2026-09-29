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

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationEventsPublisher {

    private final KafkaTemplate<String, Object> kafka;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ApplicationDomainEvents.Submitted e) {
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
        kafka.send(topic, key.toString(), payload).whenComplete((res, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} to {}", payload.getClass().getSimpleName(), topic, ex);
            } else {
                log.info("Published {} key={} partition={} offset={}", payload.getClass().getSimpleName(), key,
                        res.getRecordMetadata().partition(), res.getRecordMetadata().offset());
            }
        });
    }
}
