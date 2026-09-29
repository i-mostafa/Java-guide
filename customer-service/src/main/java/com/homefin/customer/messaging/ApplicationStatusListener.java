package com.homefin.customer.messaging;

import com.homefin.common.events.ApplicationStatusChangedEvent;
import com.homefin.common.events.Topics;
import com.homefin.customer.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Simulates a customer notification (email/SMS/push) when an application changes status.
 *
 * <p>A Kafka consumer: the equivalent of a kafkajs {@code consumer.run({ eachMessage })} handler, but wired
 * declaratively. Another service (the application/financing service) publishes the event; this class reacts to it.
 *
 * <ul>
 *   <li>{@code @Slf4j} (Lombok, compile time): generates
 *       {@code private static final Logger log = LoggerFactory.getLogger(ApplicationStatusListener.class);}.</li>
 *   <li>{@code @Component} (runtime): Spring creates one instance (singleton bean) at startup and manages it,
 *       like a NestJS {@code @Injectable()} provider. Required so Spring finds the {@code @KafkaListener} below.</li>
 *   <li>{@code @RequiredArgsConstructor} (Lombok, compile time): generates a constructor taking every
 *       {@code final} field. Spring calls that constructor and passes in the matching beans: constructor
 *       injection, just like NestJS {@code constructor(private readonly customerService: CustomerService)}.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationStatusListener {

    // "private final" = private, and assigned exactly once (in the constructor), like "private readonly" in TS.
    private final CustomerService customerService;

    // @KafkaListener (runtime): at startup Spring Kafka starts a consumer thread subscribed to this topic (group-id
    // from application.yml). For EACH record, the JSON value is deserialized into ApplicationStatusChangedEvent and
    // this method is called. If it throws, the error handler in KafkaConfig retries and then dead-letters it.
    // Topics.APPLICATION_STATUS_CHANGED is a compile-time constant from common-lib (annotation values must be).
    @KafkaListener(topics = Topics.APPLICATION_STATUS_CHANGED)
    public void onStatusChanged(ApplicationStatusChangedEvent event) {
        // "var" = local type inference (the compiler infers the type, here Customer), like "const x = ..." in TS.
        // The type is still static; var is only allowed for local variables.
        var customer = customerService.getByUserId(event.customerUserId());
        // A real implementation would call a notification provider here (SES, Twilio, FCM...).
        // SLF4J logging: {} placeholders are filled with the following arguments in order (only formatted if the
        // level is enabled), similar to pino's log.info('%s', x).
        log.info("NOTIFY customerId={} : application {} moved {} -> {}",
                customer.getId(), event.applicationId(), event.previousStatus(), event.newStatus());
    }
}
