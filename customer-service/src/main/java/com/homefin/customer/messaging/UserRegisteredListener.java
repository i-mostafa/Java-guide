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
 * Kafka consumer. Equivalent of a kafkajs {@code consumer.run({ eachMessage })} handler.
 * If this method throws, the DefaultErrorHandler (KafkaConfig) retries and then dead-letters.
 * Offsets are committed by the container only after the method returns successfully.
 *
 * <p>Role in the app: when auth-service publishes "user registered", this creates the matching customer profile.
 * Kafka delivers at-least-once, so the same event may arrive twice; {@code createFromRegistration} is written to be
 * idempotent (safe to run again).
 *
 * <ul>
 *   <li>{@code @Slf4j} (Lombok, compile time): generates a static {@code log} field (an SLF4J Logger).</li>
 *   <li>{@code @Component} (runtime): Spring instantiates this class at startup as a singleton bean.</li>
 *   <li>{@code @RequiredArgsConstructor} (Lombok, compile time): generates a constructor for the {@code final}
 *       fields; Spring uses it to inject {@code CustomerService} (constructor injection).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredListener {

    // "private final" ~ TS "private readonly": set once by the generated constructor.
    private final CustomerService customerService;

    // @KafkaListener (runtime): Spring Kafka subscribes a consumer (group "customer-service") to this topic at
    // startup and calls this method once per record, on a listener thread (not an HTTP request thread).
    // @Payload (runtime): bind the deserialized message value (JSON -> UserRegisteredEvent) to this parameter.
    // @Header(...) (runtime): bind a Kafka record metadata header, here the partition number and offset.
    // int/long are primitive number types (32-bit / 64-bit integers).
    @KafkaListener(topics = Topics.USER_REGISTERED)
    public void onUserRegistered(@Payload UserRegisteredEvent event,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset) {
        // event.eventId(): UserRegisteredEvent is a record, so accessors have no "get" prefix.
        log.info("Received UserRegisteredEvent eventId={} userId={} partition={} offset={}",
                event.eventId(), event.userId(), partition, offset);
        customerService.createFromRegistration(event);
    }
}
