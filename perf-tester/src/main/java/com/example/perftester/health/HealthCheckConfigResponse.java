package com.example.perftester.health;

public record HealthCheckConfigResponse(
        String service,
        String host,
        int port,
        boolean enabled,
        int connectionTimeoutMs,
        int intervalMs
) {
}
