package com.example.perftester.perf;

public record PerfTestResult(
        long completedMessages,
        long pendingMessages,
        double testDurationSeconds,
        double tps,
        double avgLatencyMs,
        double minLatencyMs,
        double maxLatencyMs
) {
}
