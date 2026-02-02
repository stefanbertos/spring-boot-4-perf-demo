package com.example.perftester.perf;

import com.example.perftester.kubernetes.KubernetesNodeInfo;

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
        List<KubernetesNodeInfo> kubernetesNodes
) {
    public PerfTestResult(long completedMessages, long pendingMessages, double testDurationSeconds,
                          double tps, double avgLatencyMs, double minLatencyMs, double maxLatencyMs) {
        this(completedMessages, pendingMessages, testDurationSeconds, tps, avgLatencyMs,
                minLatencyMs, maxLatencyMs, List.of(), List.of(), null, List.of());
    }

    public PerfTestResult withDashboardExports(List<String> urls, List<String> files) {
        return new PerfTestResult(completedMessages, pendingMessages, testDurationSeconds,
                tps, avgLatencyMs, minLatencyMs, maxLatencyMs, urls, files, prometheusExportFile,
                kubernetesNodes);
    }

    public PerfTestResult withPrometheusExport(String exportFile) {
        return new PerfTestResult(completedMessages, pendingMessages, testDurationSeconds,
                tps, avgLatencyMs, minLatencyMs, maxLatencyMs, dashboardUrls, dashboardExportFiles,
                exportFile, kubernetesNodes);
    }

    public PerfTestResult withKubernetesNodes(List<KubernetesNodeInfo> nodes) {
        return new PerfTestResult(completedMessages, pendingMessages, testDurationSeconds,
                tps, avgLatencyMs, minLatencyMs, maxLatencyMs, dashboardUrls, dashboardExportFiles,
                prometheusExportFile, nodes);
    }
}
