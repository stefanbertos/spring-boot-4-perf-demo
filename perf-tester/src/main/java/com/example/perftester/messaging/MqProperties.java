package com.example.perftester.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mq")
public record MqProperties(QueueProperties queue) {

    public record QueueProperties(String outbound, String inbound) {
    }
}
