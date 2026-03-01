package com.example.perftester.rest;

import java.time.Instant;

public record TrendPoint(Long id, String testId, Instant startedAt,
                         Double tps, Double p99LatencyMs, String status, String testType) {
}
