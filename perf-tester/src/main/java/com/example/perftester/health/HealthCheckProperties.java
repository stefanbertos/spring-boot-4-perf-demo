package com.example.perftester.health;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.healthcheck")
public record HealthCheckProperties(
        @DefaultValue ServiceEndpoint kafka,
        @DefaultValue ServiceEndpoint mq,
        @DefaultValue ServiceEndpoint redis,
        @DefaultValue("5000") int connectionTimeoutMs,
        @DefaultValue("60000") int intervalMs
) {
    public record ServiceEndpoint(
            @DefaultValue("localhost") String host,
            @DefaultValue("9092") int port
    ) {}
}
