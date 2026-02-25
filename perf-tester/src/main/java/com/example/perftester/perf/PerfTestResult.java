package com.example.perftester.perf;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record PerfTestResult(
        long completedMessages,
        long pendingMessages,
        double testDurationSeconds,
        double tps,
        double avgLatencyMs,
        double minLatencyMs,
        double maxLatencyMs,
        double p25LatencyMs,
        double p50LatencyMs,
        double p75LatencyMs,
        double p90LatencyMs,
        double p95LatencyMs,
        double p99LatencyMs,
        List<String> dashboardUrls,
        List<String> dashboardExportFiles,
        String prometheusExportFile,
        String kubernetesExportFile,
        Map<String, Path> dbQueryResults
) {
    public PerfTestResult(long completedMessages, long pendingMessages, double testDurationSeconds,
                          double tps, double avgLatencyMs, double minLatencyMs, double maxLatencyMs) {
        this(completedMessages, pendingMessages, testDurationSeconds, tps, avgLatencyMs,
                minLatencyMs, maxLatencyMs, 0, 0, 0, 0, 0, 0, List.of(), List.of(), null, null, null);
    }

    public PerfTestResult withPercentiles(double p25, double p50, double p75, double p90, double p95, double p99) {
        return new PerfTestResult(completedMessages, pendingMessages, testDurationSeconds,
                tps, avgLatencyMs, minLatencyMs, maxLatencyMs, p25, p50, p75, p90, p95, p99,
                dashboardUrls, dashboardExportFiles, prometheusExportFile, kubernetesExportFile, dbQueryResults);
    }

    public PerfTestResult withDashboardExports(List<String> urls, List<String> files) {
        return new PerfTestResult(completedMessages, pendingMessages, testDurationSeconds,
                tps, avgLatencyMs, minLatencyMs, maxLatencyMs,
                p25LatencyMs, p50LatencyMs, p75LatencyMs, p90LatencyMs, p95LatencyMs, p99LatencyMs,
                urls, files, prometheusExportFile, kubernetesExportFile, dbQueryResults);
    }

    public PerfTestResult withPrometheusExport(String exportFile) {
        return new PerfTestResult(completedMessages, pendingMessages, testDurationSeconds,
                tps, avgLatencyMs, minLatencyMs, maxLatencyMs,
                p25LatencyMs, p50LatencyMs, p75LatencyMs, p90LatencyMs, p95LatencyMs, p99LatencyMs,
                dashboardUrls, dashboardExportFiles, exportFile, kubernetesExportFile, dbQueryResults);
    }

    public PerfTestResult withKubernetesExport(String exportFile) {
        return new PerfTestResult(completedMessages, pendingMessages, testDurationSeconds,
                tps, avgLatencyMs, minLatencyMs, maxLatencyMs,
                p25LatencyMs, p50LatencyMs, p75LatencyMs, p90LatencyMs, p95LatencyMs, p99LatencyMs,
                dashboardUrls, dashboardExportFiles, prometheusExportFile, exportFile, dbQueryResults);
    }

    public PerfTestResult withDbQueryResults(Map<String, Path> results) {
        return new PerfTestResult(completedMessages, pendingMessages, testDurationSeconds,
                tps, avgLatencyMs, minLatencyMs, maxLatencyMs,
                p25LatencyMs, p50LatencyMs, p75LatencyMs, p90LatencyMs, p95LatencyMs, p99LatencyMs,
                dashboardUrls, dashboardExportFiles, prometheusExportFile, kubernetesExportFile, results);
    }
}
