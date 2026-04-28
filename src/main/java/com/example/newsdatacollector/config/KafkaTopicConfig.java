package com.example.newsdatacollector.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic newsKoreanTopic() {
        return TopicBuilder.name("news-korean")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic newsGlobalTopic() {
        return TopicBuilder.name("news-global")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
