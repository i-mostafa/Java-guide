package com.homefin.customer.messaging;

import com.homefin.common.events.Topics;
import com.homefin.common.events.UserRegisteredEvent;
import com.homefin.customer.customer.CustomerRepository;
import com.homefin.customer.customer.KycStatus;
import com.homefin.customer.support.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end messaging test against real Kafka + Postgres containers:
 * publish an event -> the listener consumes it -> a customer row appears.
 * Consumption is asynchronous, so we poll with Awaitility instead of Thread.sleep().
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class UserRegisteredListenerIT {

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    CustomerRepository customers;

    @Test
    void createsCustomerProfileOnceEvenIfEventIsDeliveredTwice() {
        UUID userId = UUID.randomUUID();
        var event = new UserRegisteredEvent(UUID.randomUUID(), userId, "kafka.it@example.com",
                "Kafka", "Test", Instant.now());

        kafkaTemplate.send(Topics.USER_REGISTERED, userId.toString(), event);
        kafkaTemplate.send(Topics.USER_REGISTERED, userId.toString(), event); // duplicate delivery

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            var customer = customers.findByUserId(userId);
            assertThat(customer).isPresent();
            assertThat(customer.get().getKycStatus()).isEqualTo(KycStatus.PENDING);
        });
        assertThat(customers.findAll().stream().filter(c -> c.getUserId().equals(userId))).hasSize(1);
    }
}
