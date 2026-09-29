package com.homefin.customer.config;

import com.homefin.common.events.Topics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Consumer error handling:
 * <ol>
 *   <li>retry a failing record 3 times, 1s apart (transient errors: DB blip, etc.)</li>
 *   <li>then publish it to "&lt;topic&gt;.DLT" (dead-letter topic) and move on, so one poison
 *       message never blocks the whole partition.</li>
 * </ol>
 * Spring Boot automatically plugs a single CommonErrorHandler bean into the listener factory.
 *
 * <p>In kafkajs terms: instead of writing try/catch + retry + "produce to DLT" inside every eachMessage handler,
 * one error handler bean does it for all {@code @KafkaListener} methods. The listener method throws; the listener
 * container catches it, re-delivers the record (seeks back) up to 3 more times, then hands it to the recoverer,
 * which publishes it to the DLT; then the offset is committed and consumption continues.
 *
 * <p>{@code @Configuration} (runtime): Spring calls the {@code @Bean} methods below at startup.
 * {@code @Slf4j} (Lombok, compile time): generates the {@code log} field.
 */
@Slf4j
@Configuration
public class KafkaConfig {

    // "public static final" = a constant shared by the class (like "export const DLT_SUFFIX = '.DLT'").
    // static = one per class, not per instance; final = cannot be reassigned.
    public static final String DLT_SUFFIX = ".DLT";

    // @Bean (runtime): the returned DefaultErrorHandler is registered in the container, and Boot wires it into the
    // Kafka listener container factory. The parameters are beans Spring Boot auto-configured from application.yml.
    // KafkaTemplate<?, ?> = "a KafkaTemplate with any key/value types" (wildcard generics), roughly
    // KafkaTemplate<unknown, unknown> in TS. KafkaTemplate is Spring's producer wrapper (like a kafkajs producer).
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<?, ?> jsonTemplate, ProducerFactory<?, ?> producerFactory) {
        // Records that failed DESERIALIZATION arrive as raw byte[] -> forward them untouched.
        // "new KafkaTemplate<>(...)": the empty diamond "<>" lets the compiler infer <String, byte[]> from the
        // variable type on the left. We reuse the auto-configured producer settings but swap in byte serializers.
        KafkaTemplate<String, byte[]> bytesTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                producerFactory.getConfigurationProperties(), new StringSerializer(), new ByteArraySerializer()));

        // A map from "payload class" to "template used to publish it". LinkedHashMap keeps insertion order (like a
        // JS Map), and the recoverer picks the FIRST entry whose class matches, so the specific byte[] entry must
        // come before the catch-all Object entry. "? extends Object" = any type.
        Map<Class<?>, KafkaOperations<? extends Object, ? extends Object>> templates = new LinkedHashMap<>();
        // byte[].class = the class object of the byte-array type.
        templates.put(byte[].class, bytesTemplate);
        templates.put(Object.class, jsonTemplate); // everything else (already-deserialized events) as JSON

        // The lambda decides the destination: same topic name + ".DLT". "var" lets the compiler infer the type.
        var recoverer = new DeadLetterPublishingRecoverer(templates,
                (record, ex) -> new TopicPartition(record.topic() + DLT_SUFFIX, -1)); // -1 = let Kafka pick

        // FixedBackOff(interval ms, max retries): wait 1000 ms between attempts, retry 3 times (4 deliveries total).
        // "1_000L": underscores are digit separators (like 1_000 in JS), "L" makes it a long literal.
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3L));
        // Retrying won't fix bad data - send straight to the DLT.
        // Fully-qualified name (jakarta.validation.ValidationException) used inline instead of an import.
        handler.addNotRetryableExceptions(IllegalArgumentException.class,
                jakarta.validation.ValidationException.class);
        // Callback invoked after each failed attempt, for visibility in logs.
        handler.setRetryListeners((record, ex, attempt) ->
                log.warn("Kafka delivery attempt {} failed for topic={} key={}: {}",
                        attempt, record.topic(), record.key(), ex.getMessage()));
        return handler;
    }

    // NewTopic beans are picked up by Spring Boot's KafkaAdmin at startup, which creates the topics if they
    // don't exist yet (like calling admin.createTopics() in kafkajs). One DLT per consumed topic.
    @Bean
    NewTopic userRegisteredDlt() {
        return TopicBuilder.name(Topics.USER_REGISTERED + DLT_SUFFIX).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic applicationStatusChangedDlt() {
        return TopicBuilder.name(Topics.APPLICATION_STATUS_CHANGED + DLT_SUFFIX).partitions(1).replicas(1).build();
    }
}
