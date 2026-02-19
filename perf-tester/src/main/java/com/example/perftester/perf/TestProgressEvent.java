package com.example.perftester.perf;

public record TestProgressEvent(
        String testRunId,
        String status,
        long sentCount,
        long completedCount,
        long totalCount,
        double progressPercent,
        double tps,
        double avgLatencyMs,
        double minLatencyMs,
        double maxLatencyMs,
        double elapsedSeconds) {
}
