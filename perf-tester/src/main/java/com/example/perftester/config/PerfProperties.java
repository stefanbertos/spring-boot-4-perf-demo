package com.example.perftester.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.perf")
public record PerfProperties(
        long metricsPropagationDelayMs,
        long tpsWindowMs,
        long grafanaBufferBeforeMs,
        long grafanaBufferAfterMs,
        long prometheusBufferSeconds,
        int prometheusStepSeconds
) {
}
