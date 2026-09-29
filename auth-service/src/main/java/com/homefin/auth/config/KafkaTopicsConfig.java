package com.homefin.auth.config;

import com.homefin.common.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Spring Boot's KafkaAdmin creates these topics at startup if they don't exist.
 *
 * <p>Every {@code NewTopic} bean is picked up by KafkaAdmin. Node analogy: calling
 * {@code kafkajs admin.createTopics({ topics: [...] })} in a boot script.
 */
@Configuration
public class KafkaTopicsConfig {

    // A @Bean of type NewTopic -> declares the topic. 3 partitions allow up to 3 parallel consumers
    // in one group; replicas(1) is only OK for a single local broker.
    @Bean
    NewTopic userRegisteredTopic() {
        return TopicBuilder.name(Topics.USER_REGISTERED).partitions(3).replicas(1).build();
    }
}
