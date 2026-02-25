package com.example.perftester.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public record KafkaAdminProperties(String bootstrapServers) {
}
