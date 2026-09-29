package com.homefin.application.config;

import com.homefin.common.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    NewTopic applicationSubmittedTopic() {
        return TopicBuilder.name(Topics.APPLICATION_SUBMITTED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic applicationStatusChangedTopic() {
        return TopicBuilder.name(Topics.APPLICATION_STATUS_CHANGED).partitions(3).replicas(1).build();
    }
}
