package com.example.WorkHub.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic configuration.
 * Creates the "report-requests" topic on startup if it doesn't already exist.
 */
@Configuration
public class KafkaConfig {

    public static final String REPORT_TOPIC = "report-requests";

    @Bean
    public NewTopic reportRequestsTopic() {
        return TopicBuilder.name(REPORT_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
