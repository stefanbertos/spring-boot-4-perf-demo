package com.example.perftester.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ibm.mq")
public record IbmMqConnectionProperties(
        String queueManager,
        String channel,
        String connName,
        String user,
        String password) {
}
