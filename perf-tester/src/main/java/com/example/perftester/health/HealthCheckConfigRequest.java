package com.example.perftester.health;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record HealthCheckConfigRequest(
        @NotBlank String host,
        @Min(1) @Max(65535) int port,
        boolean enabled,
        @Min(100) int connectionTimeoutMs,
        @Min(1000) int intervalMs
) {
}
