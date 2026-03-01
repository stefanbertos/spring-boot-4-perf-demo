package com.example.perftester.rest;

import java.time.Instant;
import java.util.List;

public record TestRunDetailResponse(
        Long id,
        String testRunId,
        String testId,
        String status,
        int messageCount,
        long completedCount,
        Double tps,
        Double avgLatencyMs,
        Double minLatencyMs,
        Double maxLatencyMs,
        Double p25LatencyMs,
        Double p50LatencyMs,
        Double p75LatencyMs,
        Double p90LatencyMs,
        Double p95LatencyMs,
        Double p99LatencyMs,
        Long timeoutCount,
        String testType,
        String thresholdStatus,
        String thresholdResults,
        Long durationMs,
        Instant startedAt,
        Instant completedAt,
        String zipFilePath,
        List<String> tags) {
}
