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
 *  1. retry a failing record 3 times, 1s apart (transient errors: DB blip, etc.)
 *  2. then publish it to "<topic>.DLT" (dead-letter topic) and move on, so one poison
 *     message never blocks the whole partition.
 * Spring Boot automatically plugs a single CommonErrorHandler bean into the listener factory.
 */
@Slf4j
@Configuration
public class KafkaConfig {

    public static final String DLT_SUFFIX = ".DLT";

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<?, ?> jsonTemplate, ProducerFactory<?, ?> producerFactory) {
        // Records that failed DESERIALIZATION arrive as raw byte[] -> forward them untouched.
        KafkaTemplate<String, byte[]> bytesTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                producerFactory.getConfigurationProperties(), new StringSerializer(), new ByteArraySerializer()));

        Map<Class<?>, KafkaOperations<? extends Object, ? extends Object>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, bytesTemplate);
        templates.put(Object.class, jsonTemplate); // everything else (already-deserialized events) as JSON

        var recoverer = new DeadLetterPublishingRecoverer(templates,
                (record, ex) -> new TopicPartition(record.topic() + DLT_SUFFIX, -1)); // -1 = let Kafka pick

        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3L));
        // Retrying won't fix bad data - send straight to the DLT.
        handler.addNotRetryableExceptions(IllegalArgumentException.class,
                jakarta.validation.ValidationException.class);
        handler.setRetryListeners((record, ex, attempt) ->
                log.warn("Kafka delivery attempt {} failed for topic={} key={}: {}",
                        attempt, record.topic(), record.key(), ex.getMessage()));
        return handler;
    }

    @Bean
    NewTopic userRegisteredDlt() {
        return TopicBuilder.name(Topics.USER_REGISTERED + DLT_SUFFIX).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic applicationStatusChangedDlt() {
        return TopicBuilder.name(Topics.APPLICATION_STATUS_CHANGED + DLT_SUFFIX).partitions(1).replicas(1).build();
    }
}
