package com.example.perftester.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.monitoring")
public record MonitoringProperties(String outboundQueue, String inboundQueue,
        String kafkaRequestConsumerGroup, String kafkaResponseConsumerGroup) {
}
