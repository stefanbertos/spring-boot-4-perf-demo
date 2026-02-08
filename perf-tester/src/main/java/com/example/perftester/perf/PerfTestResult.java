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
        List<String> dashboardUrls,
        List<String> dashboardExportFiles,
        String prometheusExportFile,
        String kubernetesExportFile
) {
    public PerfTestResult(long completedMessages, long pendingMessages, double testDurationSeconds,
                          double tps, double avgLatencyMs, double minLatencyMs, double maxLatencyMs) {
        this(completedMessages, pendingMessages, testDurationSeconds, tps, avgLatencyMs,
                minLatencyMs, maxLatencyMs, List.of(), List.of(), null, null);
    }

    public PerfTestResult withDashboardExports(List<String> urls, List<String> files) {
        return new PerfTestResult(completedMessages, pendingMessages, testDurationSeconds,
                tps, avgLatencyMs, minLatencyMs, maxLatencyMs, urls, files, prometheusExportFile,
                kubernetesExportFile);
    }

    public PerfTestResult withPrometheusExport(String exportFile) {
        return new PerfTestResult(completedMessages, pendingMessages, testDurationSeconds,
                tps, avgLatencyMs, minLatencyMs, maxLatencyMs, dashboardUrls, dashboardExportFiles,
                exportFile, kubernetesExportFile);
    }

    public PerfTestResult withKubernetesExport(String exportFile) {
        return new PerfTestResult(completedMessages, pendingMessages, testDurationSeconds,
                tps, avgLatencyMs, minLatencyMs, maxLatencyMs, dashboardUrls, dashboardExportFiles,
                prometheusExportFile, exportFile);
    }
}
