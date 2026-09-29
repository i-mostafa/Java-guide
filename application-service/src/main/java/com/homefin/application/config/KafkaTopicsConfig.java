package com.homefin.application.config;

import com.homefin.common.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the Kafka topics this service PRODUCES to.
 *
 * <p>Role: at startup Spring Kafka's {@code KafkaAdmin} collects every {@code NewTopic} bean and creates
 * the missing topics on the broker (existing topics are left alone). TS analogy: calling
 * {@code admin.createTopics({ topics: [...] })} with kafkajs during bootstrap. In production topics are
 * often created by infra instead; this keeps local dev "just works".
 */
// @Configuration (runtime): its @Bean methods are called by Spring at startup.
@Configuration
public class KafkaTopicsConfig {

    // @Bean (runtime): the returned NewTopic becomes a bean; the method name is the bean name.
    // Topics.APPLICATION_SUBMITTED is a shared constant (a "public static final String") from common-lib,
    // so producers and consumers can't misspell topic names.
    // partitions(3): up to 3 consumers in one group can read in parallel. replicas(1): fine for local dev only.
    @Bean
    NewTopic applicationSubmittedTopic() {
        return TopicBuilder.name(Topics.APPLICATION_SUBMITTED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic applicationStatusChangedTopic() {
        return TopicBuilder.name(Topics.APPLICATION_STATUS_CHANGED).partitions(3).replicas(1).build();
    }
}
