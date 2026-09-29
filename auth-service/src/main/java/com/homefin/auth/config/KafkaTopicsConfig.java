package com.homefin.auth.config;

import com.homefin.common.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Spring Boot's KafkaAdmin creates these topics at startup if they don't exist. */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    NewTopic userRegisteredTopic() {
        return TopicBuilder.name(Topics.USER_REGISTERED).partitions(3).replicas(1).build();
    }
}
