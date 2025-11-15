package com.example.shortener.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    /**
     * Creates the "clicks" topic in Kafka if it does not already exist.
     * Topic configuration:
     * - Name: "clicks"
     * - Partitions: 6 (allows parallelism for multiple consumers)
     * - Replication factor: 3 (for fault tolerance and durability)
     *
     * @return NewTopic object representing the "clicks" topic
     */
    @Bean
    public NewTopic clicksTopic() {
        return new NewTopic("clicks", 6, (short) 3);
    }
}
