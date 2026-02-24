package com.example.perftester.perf;

import java.util.List;

public record PerfTestResult(
        long completedMessages,
        long pendingMessages,
        double testDurationSeconds,
        double tps,
        double avgLatencyMs,
        double minLatencyMs,
        double maxLatencyMs,
        double p50LatencyMs,
        double p90LatencyMs,
        double p95LatencyMs,
        double p99LatencyMs,
        List<String> dashboardUrls,
        List<String> dashboardExportFiles,
        String prometheusExportFile,
        String kubernetesExportFile
) {
    public PerfTestResult(long completedMessages, long pendingMessages, double testDurationSeconds,
                          double tps, double avgLatencyMs, double minLatencyMs, double maxLatencyMs) {
        this(completedMessages, pendingMessages, testDurationSeconds, tps, avgLatencyMs,
                minLatencyMs, maxLatencyMs, 0, 0, 0, 0, List.of(), List.of(), null, null);
    }

    public PerfTestResult withPercentiles(double p50, double p90, double p95, double p99) {
        return new PerfTestResult(completedMessages, pendingMessages, testDurationSeconds,
                tps, avgLatencyMs, minLatencyMs, maxLatencyMs, p50, p90, p95, p99,
                dashboardUrls, dashboardExportFiles, prometheusExportFile, kubernetesExportFile);
    }

    public PerfTestResult withDashboardExports(List<String> urls, List<String> files) {
        return new PerfTestResult(completedMessages, pendingMessages, testDurationSeconds,
                tps, avgLatencyMs, minLatencyMs, maxLatencyMs,
                p50LatencyMs, p90LatencyMs, p95LatencyMs, p99LatencyMs,
                urls, files, prometheusExportFile, kubernetesExportFile);
    }

    public PerfTestResult withPrometheusExport(String exportFile) {
        return new PerfTestResult(completedMessages, pendingMessages, testDurationSeconds,
                tps, avgLatencyMs, minLatencyMs, maxLatencyMs,
                p50LatencyMs, p90LatencyMs, p95LatencyMs, p99LatencyMs,
                dashboardUrls, dashboardExportFiles, exportFile, kubernetesExportFile);
    }

    public PerfTestResult withKubernetesExport(String exportFile) {
        return new PerfTestResult(completedMessages, pendingMessages, testDurationSeconds,
                tps, avgLatencyMs, minLatencyMs, maxLatencyMs,
                p50LatencyMs, p90LatencyMs, p95LatencyMs, p99LatencyMs,
                dashboardUrls, dashboardExportFiles, prometheusExportFile, exportFile);
    }
}
