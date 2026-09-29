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

// Static imports: call assertThat(...) and await() directly, like named imports of helper functions.
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end messaging test against real Kafka + Postgres containers:
 * publish an event -> the listener consumes it -> a customer row appears.
 * Consumption is asynchronous, so we poll with Awaitility instead of Thread.sleep().
 *
 * <p>The "IT" suffix marks an integration test (by convention run by the Maven Failsafe plugin in
 * {@code mvn verify}, separately from fast unit tests). Needs Docker.
 *
 * <ul>
 *   <li>{@code @SpringBootTest} (runtime, JUnit extension): starts the WHOLE application context, like booting the
 *       real NestJS app in a test. Slow but realistic: real listeners, real DB, real Kafka.</li>
 *   <li>{@code @ActiveProfiles("test")}: also load application-test.yml (disables Config Server, Eureka,
 *       tracing).</li>
 *   <li>{@code @Import(TestcontainersConfig.class)}: add the container beans, which redirect the datasource and
 *       Kafka config to the Docker containers.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class UserRegisteredListenerIT {

    // @Autowired (runtime): field injection; Spring sets this field from the context before the test runs.
    // In production code prefer constructor injection; in tests field injection is the common, concise style.
    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    CustomerRepository customers;

    @Test
    void createsCustomerProfileOnceEvenIfEventIsDeliveredTwice() {
        UUID userId = UUID.randomUUID();
        var event = new UserRegisteredEvent(UUID.randomUUID(), userId, "kafka.it@example.com",
                "Kafka", "Test", Instant.now());

        // Produce the event twice with the same key (same key -> same partition -> processed in order).
        kafkaTemplate.send(Topics.USER_REGISTERED, userId.toString(), event);
        kafkaTemplate.send(Topics.USER_REGISTERED, userId.toString(), event); // duplicate delivery

        // Awaitility: re-run the lambda until its assertions pass or 20s elapse (then fail with the last error).
        // Like waitFor(() => expect(...)) in Testing Library. Duration.ofSeconds(20) = a java.time.Duration.
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            var customer = customers.findByUserId(userId);
            // AssertJ understands Optional: isPresent() asserts it holds a value; get() unwraps it.
            assertThat(customer).isPresent();
            assertThat(customer.get().getKycStatus()).isEqualTo(KycStatus.PENDING);
        });
        // Streams: stream().filter(lambda) is like array.filter(...), evaluated lazily. hasSize(1) proves the
        // duplicate event did NOT create a second row. c -> ... is a one-parameter lambda.
        assertThat(customers.findAll().stream().filter(c -> c.getUserId().equals(userId))).hasSize(1);
    }
}
