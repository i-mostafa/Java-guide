package com.homefin.customer.messaging;

import com.homefin.common.events.ApplicationStatusChangedEvent;
import com.homefin.common.events.Topics;
import com.homefin.customer.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Simulates a customer notification (email/SMS/push) when an application changes status. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationStatusListener {

    private final CustomerService customerService;

    @KafkaListener(topics = Topics.APPLICATION_STATUS_CHANGED)
    public void onStatusChanged(ApplicationStatusChangedEvent event) {
        var customer = customerService.getByUserId(event.customerUserId());
        // A real implementation would call a notification provider here (SES, Twilio, FCM...).
        log.info("NOTIFY customerId={} : application {} moved {} -> {}",
                customer.getId(), event.applicationId(), event.previousStatus(), event.newStatus());
    }
}
