package com.homefin.customer.messaging;

import com.homefin.common.events.Topics;
import com.homefin.common.events.UserRegisteredEvent;
import com.homefin.customer.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer. Equivalent of a kafkajs `consumer.run({ eachMessage })` handler.
 * If this method throws, the DefaultErrorHandler (KafkaConfig) retries and then dead-letters.
 * Offsets are committed by the container only after the method returns successfully.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredListener {

    private final CustomerService customerService;

    @KafkaListener(topics = Topics.USER_REGISTERED)
    public void onUserRegistered(@Payload UserRegisteredEvent event,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received UserRegisteredEvent eventId={} userId={} partition={} offset={}",
                event.eventId(), event.userId(), partition, offset);
        customerService.createFromRegistration(event);
    }
}
